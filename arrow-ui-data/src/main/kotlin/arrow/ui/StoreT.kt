package arrow.ui

import arrow.Kind
import arrow.core.andThen
import arrow.core.constant
import arrow.core.identity
import arrow.higherkind
import arrow.typeclasses.Applicative
import arrow.typeclasses.Comonad
import arrow.typeclasses.Functor
import arrow.typeclasses.Monoid

/**
 * `StoreT<S, W, A>` is a comonad transformer that extends the context of a value
 * in the base comonad `W` so that the value depends on a position of type `S`.
 *
 * @param S the space explored by the Comonad
 * @param W the base comonad
 * @param A the value of the computation
 * @param state the current position in space
 * @param render the computation that returns the value for the current position
 */
@higherkind
class StoreT<S, W, A>(
  val state: S,
  val render: Kind<W, (S) -> A>
) : StoreTOf<S, W, A>, StoreTKindedJ<S, W, A> {

  /**
   * Map current value [A] given function [f]
   *
   * @param FF [Functor] for the [Comonad] [W]
   * @param f the function to apply
   */
  fun <B> map(FW: Functor<W>, f: (A) -> B): StoreT<S, W, B> = with(FW) {
    StoreT(state, render.map { ff -> ff.andThen(f) })
  }

  /**
   * Apply a function `(S) -> B` that operates withing the [StoreT] context
   *
   * @param AW [Applicative] for the [Comonad] [W]
   * @param MS [Monoid] for combining positions in space [S]
   * @param ff function with the [StoreT] context
   */
  fun <B> ap(AW: Applicative<W>, MS: Monoid<S>, ff: StoreT<S, W, (A) -> B>): StoreT<S, W, B> = with(MS) {
    StoreT(ff.state.combine(state), AW.mapN(ff.render, render) { (rf, ra) -> { s -> rf(s)(ra(s)) } })
  }

  /**
   * Map the value [B] from another [StoreT] object for the same space [S] and [Comonad] [W] and rebuild the structure.
   *
   * @param CMW [Comonad] for the context [W]
   * @param f the function to apply
   */
  fun <B> coflatMap(CMW: Comonad<W>, f: (StoreT<S, W, A>) -> B): StoreT<S, W, B> = with(CMW) {
    StoreT(state, render.coflatMap { wa -> { s -> f(StoreT(s, wa)) } })
  }

  /**
   * Extract the [A] value from the current position in space [S]
   *
   * @param CMW [Comonad] for the context [W]
   */
  fun extract(CMW: Comonad<W>): A = with(CMW) { render.extract()(state) }

  /**
   * Unfolds the space [S] based on the current position
   *
   * @param CMW [Comonad] for the context [W]
   */
  fun duplicate(CMW: Comonad<W>): StoreT<S, W, StoreT<S, W, A>> = coflatMap(CMW, ::identity)

  /**
   * Returns the current position in space [S]
   */
  fun position(): S = state

  /**
   * Computes the value [A] for position [state]
   *
   * @param CMW [Comonad] for the context [W]
   * @param state a position in space [S]
   */
  fun peek(CMW: Comonad<W>, state: S): A = with(CMW) { render.extract()(state) }

  /**
   * Obtains the comonadic value, removing the [StoreT] support
   *
   * @param FW [Functor] for the [Comonad] [W]
   */
  fun lower(FW: Functor<W>): Kind<W, A> = with(FW) { render.map { f -> f(state) } }

  /**
   * Move the [StoreT] into another position
   *
   * @param CMW [Comonad] for the context [W]
   * @param newState new position for the [StoreT]
   */
  fun move(CMW: Comonad<W>, newState: S): StoreT<S, W, A> = duplicate(CMW).peek(CMW, newState)

  companion object {
    fun <S, W, A> just(AW: Applicative<W>, MS: Monoid<S>, a: A): StoreT<S, W, A> = with(MS) {
      StoreT(empty(), AW.just(constant(a)))
    }
  }
}
