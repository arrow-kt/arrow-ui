package arrow.ui.extensions

import arrow.Kind
import arrow.core.ForId
import arrow.core.Id
import arrow.core.extensions.id.applicative.applicative
import arrow.core.extensions.id.comonad.comonad
import arrow.core.extensions.id.functor.functor
import arrow.extension
import arrow.typeclasses.Applicative
import arrow.typeclasses.Comonad
import arrow.typeclasses.Functor
import arrow.typeclasses.Monoid
import arrow.ui.StoreApi
import arrow.ui.StoreT
import arrow.ui.StoreTPartialOf
import arrow.ui.extensions.storet.applicative.applicative
import arrow.ui.extensions.storet.comonad.comonad
import arrow.ui.extensions.storet.functor.functor
import arrow.ui.fix
import arrow.undocumented

@extension
@undocumented
interface StoreTFunctor<S, W> : Functor<StoreTPartialOf<S, W>> {
  fun FW(): Functor<W>

  override fun <A, B> Kind<StoreTPartialOf<S, W>, A>.map(f: (A) -> B): StoreT<S, W, B> =
    fix().map(FW(), f)
}

@extension
@undocumented
interface StoreTComonad<S, W> : Comonad<StoreTPartialOf<S, W>>, StoreTFunctor<S, W> {
  fun CMW(): Comonad<W>

  override fun FW(): Functor<W> = CMW()

  override fun <A, B> Kind<StoreTPartialOf<S, W>, A>.coflatMap(f: (Kind<StoreTPartialOf<S, W>, A>) -> B): StoreT<S, W, B> =
    fix().coflatMap(CMW(), f)

  override fun <A> Kind<StoreTPartialOf<S, W>, A>.extract(): A =
    fix().extract(CMW())
}

@extension
@undocumented
interface StoreTApplicative<S, W> : Applicative<StoreTPartialOf<S, W>>, StoreTFunctor<S, W> {
  fun AW(): Applicative<W>

  fun MS(): Monoid<S>

  override fun FW(): Functor<W> = AW()

  override fun <A, B> Kind<StoreTPartialOf<S, W>, A>.map(f: (A) -> B): StoreT<S, W, B> =
    fix().map(FW(), f)

  override fun <A> just(a: A): StoreT<S, W, A> =
    StoreT.just(AW(), MS(), a)

  override fun <A, B> Kind<StoreTPartialOf<S, W>, A>.ap(ff: Kind<StoreTPartialOf<S, W>, (A) -> B>): StoreT<S, W, B> =
    fix().ap(AW(), MS(), ff.fix())
}

fun <S> StoreApi.functor(): Functor<StoreTPartialOf<S, ForId>> = StoreT.functor(Id.functor())

fun <S> StoreApi.applicative(MS: Monoid<S>): Applicative<StoreTPartialOf<S, ForId>> = StoreT.applicative(Id.applicative(), MS)

fun <S> StoreApi.comonad(): Comonad<StoreTPartialOf<S, ForId>> = StoreT.comonad(Id.comonad())
