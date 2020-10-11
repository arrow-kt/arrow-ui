package arrow.ui

import arrow.core.Tuple2
import arrow.core.andThen
import arrow.core.toT
import arrow.higherkind
import arrow.mtl.State
import arrow.mtl.run
import arrow.typeclasses.Monoid

@higherkind
data class Moore<E, V>(val view: V, val handle: (E) -> Moore<E, V>) : MooreOf<E, V>, MooreKindedJ<E, V> {

  fun <A> coflatMap(f: (Moore<E, V>) -> A): Moore<E, A> =
    Moore(f(Moore(view, handle))) { update -> handle(update).coflatMap(f) }

  fun <A> map(f: (V) -> A): Moore<E, A> =
    Moore(f(view)) { update -> handle(update).map(f) }

  fun <EE> contramapInput(f: (EE) -> E): Moore<EE, V> =
    Moore(view, f.andThen { x -> handle(x).contramapInput(f) })

  fun extract(): V = view

  override fun toString() = "Moore(view=$view, handle=(E) -> Moore<E, V>)"

  companion object {
    fun <E, V, S> unfold(state: S, next: (S) -> Tuple2<V, (E) -> S>): Moore<E, V> {
      val (a, transition) = next(state)
      return Moore(a) { input -> unfold(transition(input), next) }
    }

    fun <E, V, S> from(initialState: S, render: (S) -> V, update: (S, E) -> S): Moore<E, V> =
      unfold(initialState) { state -> render(state) toT { input -> update(state, input) } }

    fun <E, V, S> from(initialState: S, render: (S) -> V, update: (E) -> State<S, S>): Moore<E, V> =
      from(initialState, render, { s, e -> update(e).run(s).a })

    fun <E> log(ME: Monoid<E>): Moore<E, E> {
      fun rec(m: E): Moore<E, E> = ME.run { Moore(m) { a -> rec(m.combine(a)) } }
      return ME.run { rec(empty()) }
    }
  }
}
