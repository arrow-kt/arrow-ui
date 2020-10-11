package arrow.ui.extensions

import arrow.Kind
import arrow.core.Either
import arrow.core.ForId
import arrow.extension
import arrow.typeclasses.Applicative
import arrow.typeclasses.Apply
import arrow.typeclasses.Comonad
import arrow.typeclasses.Functor
import arrow.typeclasses.Monad
import arrow.typeclasses.MonadSyntax
import arrow.ui.Co
import arrow.ui.CoApi
import arrow.ui.CoPartialOf
import arrow.ui.CoT
import arrow.ui.CoTPartialOf
import arrow.ui.extensions.cot.applicative.applicative
import arrow.ui.extensions.cot.functor.functor
import arrow.ui.extensions.cot.monad.monad
import arrow.ui.fix

@extension
interface CoTFunctor<W, M> : Functor<CoTPartialOf<W, M>> {
  override fun <A, B> Kind<CoTPartialOf<W, M>, A>.map(f: (A) -> B): CoT<W, M, B> =
    fix().map(f)
}

@extension
interface CoTApply<W, M> : Apply<CoTPartialOf<W, M>>, CoTFunctor<W, M> {
  override fun <A, B> Kind<CoTPartialOf<W, M>, A>.ap(ff: Kind<CoTPartialOf<W, M>, (A) -> B>): CoT<W, M, B> =
    fix().ap(ff)
}

@extension
interface CoTApplicative<W, M> : Applicative<CoTPartialOf<W, M>>, CoTApply<W, M> {
  fun CMW(): Comonad<W>

  override fun <A> just(a: A): CoT<W, M, A> =
    CoT.just(CMW(), a)

  override fun <A, B> Kind<CoTPartialOf<W, M>, A>.map(f: (A) -> B): CoT<W, M, B> =
    fix().map(f)

  override fun <A, B> Kind<CoTPartialOf<W, M>, A>.ap(ff: Kind<CoTPartialOf<W, M>, (A) -> B>): CoT<W, M, B> =
    fix().ap(ff)
}

@extension
interface CoTMonad<W, M> : Monad<CoTPartialOf<W, M>>, CoTApplicative<W, M> {
  override fun CMW(): Comonad<W>

  override fun <A, B> tailRecM(a: A, f: (A) -> Kind<CoTPartialOf<W, M>, Either<A, B>>): CoT<W, M, B> =
    CoT.tailRecM(CMW(), a, f)

  override fun <A, B> Kind<CoTPartialOf<W, M>, A>.ap(ff: Kind<CoTPartialOf<W, M>, (A) -> B>): CoT<W, M, B> =
    fix().ap(ff)

  override fun <A, B> Kind<CoTPartialOf<W, M>, A>.flatMap(f: (A) -> Kind<CoTPartialOf<W, M>, B>): CoT<W, M, B> =
    fix().flatMap(f)

  override fun <A, B> Kind<CoTPartialOf<W, M>, A>.map(f: (A) -> B): CoT<W, M, B> =
    fix().map(f)
}

fun <M, W, A> CoT.Companion.fx(CMW: Comonad<W>, c: suspend MonadSyntax<CoTPartialOf<W, M>>.() -> A): CoT<W, M, A> =
  CoT.monad<W, M>(CMW).fx.monad(c).fix()

fun <W> CoApi.functor(): Functor<CoTPartialOf<W, ForId>> = CoT.functor()

fun <W> CoApi.applicative(CMW: Comonad<W>): Applicative<CoTPartialOf<W, ForId>> = CoT.applicative(CMW)

fun <W> CoApi.monad(CMW: Comonad<W>): Monad<CoTPartialOf<W, ForId>> = CoT.monad(CMW)

fun <W, A> CoApi.fx(CMW: Comonad<W>, c: suspend MonadSyntax<CoPartialOf<W>>.() -> A): Co<W, A> =
  CoApi.monad(CMW).fx.monad(c).fix()
