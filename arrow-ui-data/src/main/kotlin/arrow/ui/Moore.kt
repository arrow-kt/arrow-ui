package arrow.ui

import arrow.core.Tuple2
import arrow.core.andThen
import arrow.core.toT
import arrow.higherkind
import arrow.mtl.State
import arrow.mtl.run
import arrow.typeclasses.Monoid

/**
 * Moore represents Moore machines that hold values of type `V` and handle inputs of type `E`
 *
 * @param view Value hold in this Moore machine
 * @param handle Function to handle inputs to the Moore machine
 */
@higherkind
data class Moore<E, V>(val view: V, val handle: (E) -> Moore<E, V>) : MooreOf<E, V>, MooreKindedJ<E, V> {

  /**
   * Map the value [A] from another [Moore] object for the same inputs [E] and rebuild the structure.
   *
   * @param f the function to apply
   */
  fun <A> coflatMap(f: (Moore<E, V>) -> A): Moore<E, A> =
    Moore(f(Moore(view, handle))) { update -> handle(update).coflatMap(f) }

  /**
   * Map current value [A] given function [f]
   *
   * @param f the function to apply
   */
  fun <A> map(f: (V) -> A): Moore<E, A> =
    Moore(f(view)) { update -> handle(update).map(f) }

  /**
   * Transforms the inputs this machine is able to handle
   *
   * @param f Transforming function
   */
  fun <EE> contramapInput(f: (EE) -> E): Moore<EE, V> =
    Moore(view, f.andThen { x -> handle(x).contramapInput(f) })

  fun extract(): V = view

  override fun toString() = "Moore(view=$view, handle=(E) -> Moore<E, V>)"

  companion object {
    /**
     * Creates a Moore machine from a hidden initial state and a function that
     * provides the next value and handling function.
     *
     * @param state Initial state
     * @param next Function to determine the next value and handling function
     */
    fun <E, V, S> unfold(state: S, next: (S) -> Tuple2<V, (E) -> S>): Moore<E, V> {
      val (a, transition) = next(state)
      return Moore(a) { input -> unfold(transition(input), next) }
    }

    /**
     * Creates a Moore machine from an initial state, a rendering function and an update function
     *
     * @param initialState Initial state
     * @param render Rendering function
     * @param update Update function
     */
    fun <E, V, S> from(initialState: S, render: (S) -> V, update: (S, E) -> S): Moore<E, V> =
      unfold(initialState) { state -> render(state) toT { input -> update(state, input) } }

    /**
     * Creates a Moore machine from an initial state, a rendering function and an update function
     *
     * @param initialState Initial state
     * @param render Rendering function
     * @param update Update function
     */
    fun <E, V, S> from(initialState: S, render: (S) -> V, update: (E) -> State<S, S>): Moore<E, V> =
      from(initialState, render, { s, e -> update(e).run(s).a })

    /**
     * Creates a Moore machine that logs the inputs it receives
     *
     * @param ME [Monoid] for type [E]
     */
    fun <E> log(ME: Monoid<E>): Moore<E, E> {
      fun rec(m: E): Moore<E, E> = ME.run { Moore(m) { a -> rec(m.combine(a)) } }
      return ME.run { rec(empty()) }
    }
  }
}
