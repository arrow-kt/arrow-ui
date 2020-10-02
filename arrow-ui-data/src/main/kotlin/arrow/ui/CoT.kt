package arrow.ui

import arrow.Kind
import arrow.core.Either
import arrow.core.Function1
import arrow.core.andThen
import arrow.core.compose
import arrow.higherkind
import arrow.typeclasses.Comonad
import arrow.typeclasses.Functor

typealias TransitionT<W, M, A> = CoT<W, M, A>

@higherkind
class CoT<W, M, A>(
  private val CMW: Comonad<W>,
  private val cow: (Kind<W, (A) -> Kind<M, Any>>) -> Kind<M, Any>
) : CoTOf<W, M, A>, CoTKindedJ<W, M, A>, Comonad<W> by CMW {

  fun <B> map(f: (A) -> B): CoT<W, M, B> =
    CoT(CMW) { b -> runT(b.map { bb -> f.andThen(bb) }) }

  fun <B> ap(ff: CoTOf<W, M, (A) -> B>): CoT<W, M, B> =
    CoT(CMW) { w ->
      ff.fix().cow(w.coflatMap { wf ->
        Function1<(A) -> B, Kind<M, Any>> { g -> cow(wf.map { ff -> ff.compose(g) }) }.f
      })
    }

  fun <B> flatMap(f: (A) -> CoTOf<W, M, B>): CoT<W, M, B> =
    CoT(CMW) { w ->
      cow(w.coflatMap { wa ->
        { a: A -> f(a).fix().runT(wa) }
      })
    }

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

    fun <W, M> functor() = object : Functor<CoTPartialOf<W, M>> {
      override fun <A, B> Kind<CoTPartialOf<W, M>, A>.map(f: (A) -> B): Kind<CoTPartialOf<W, M>, B> =
        fix().map(f)
    }
  }
}
