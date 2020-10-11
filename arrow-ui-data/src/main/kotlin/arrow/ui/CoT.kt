package arrow.ui

import arrow.Kind
import arrow.core.Either
import arrow.core.Function1
import arrow.core.andThen
import arrow.core.compose
import arrow.higherkind
import arrow.typeclasses.Comonad

/**
 * Alias for [CoT]
 */
typealias TransitionT<W, M, A> = CoT<W, M, A>

/**
 * `CoT` gives you "the best" pairing [Monad] transformer for any [Comonad] `W`
 * In other words, an explorer for the state space given by [W].
 */
@higherkind
class CoT<W, M, A>(
  private val CMW: Comonad<W>,
  internal val cow: (Kind<W, (A) -> Kind<M, Any>>) -> Kind<M, Any>
) : CoTOf<W, M, A>, CoTKindedJ<W, M, A>, Comonad<W> by CMW {

  /**
   * Map current value [A] given function [f]
   *
   * @param f the function to apply
   */
  fun <B> map(f: (A) -> B): CoT<W, M, B> =
    CoT(CMW) { b -> runT(b.map { bb -> f.andThen(bb) }) }

  /**
   * Apply a function `(S) -> B` that operates withing the [CoT] context
   *
   * @param ff function with the [CoT] context
   */
  fun <B> ap(ff: CoTOf<W, M, (A) -> B>): CoT<W, M, B> =
    CoT(CMW) { w ->
      ff.fix().cow(w.coflatMap { wf ->
        Function1<(A) -> B, Kind<M, Any>> { g -> cow(wf.map { ff -> ff.compose(g) }) }.f
      })
    }

  /**
   * Map the value [A] to another [CoT] object for the same state [S] and context [F] and flatten the structure.
   *
   * @param f the function to apply.
   */
  fun <B> flatMap(f: (A) -> CoTOf<W, M, B>): CoT<W, M, B> =
    CoT(CMW) { w ->
      cow(w.coflatMap { wa ->
        { a: A -> f(a).fix().runT(wa) }
      })
    }

  /**
   * Runs the inner function
   *
   * @param w argument for the inner function
   */
  fun <R> runT(w: Kind<W, (A) -> Kind<M, R>>): Kind<M, R> =
    (cow as (Kind<W, (A) -> Kind<M, R>>) -> Kind<M, R>)(w)

  companion object {
    fun <W, M, A> just(CMW: Comonad<W>, a: A): CoT<W, M, A> = CMW.run {
      CoT(CMW) { w -> w.extract()(a) }
    }

    fun <W, M, A, B> tailRecM(CMW: Comonad<W>, a: A, f: (A) -> CoTOf<W, M, Either<A, B>>): CoT<W, M, B> =
      CMW.run {
        f(a).fix().flatMap { either ->
          either.fold(
            { aa -> tailRecM(CMW, aa, f) },
            { b -> just(CMW, b) }
          )
        }
      }
  }
}
