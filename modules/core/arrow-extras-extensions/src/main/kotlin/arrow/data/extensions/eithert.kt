package arrow.data.extensions

import arrow.Kind
import arrow.core.*
import arrow.core.extensions.either.foldable.foldable
import arrow.core.extensions.either.monad.monad
import arrow.core.extensions.either.traverse.traverse
import arrow.data.*
import arrow.data.extensions.eithert.monadThrow.monadThrow
import arrow.extension
import arrow.typeclasses.*
import arrow.undocumented

@extension
@undocumented
interface EitherTFunctor<F, L> : Functor<EitherTPartialOf<F, L>> {

  fun FF(): Functor<F>

  override fun <A, B> EitherTOf<F, L, A>.map(f: (A) -> B): EitherT<F, L, B> =
    fix().map(FF(), f)
}

@extension
@undocumented
interface EitherTApplicative<F, L> : Applicative<EitherTPartialOf<F, L>>, EitherTFunctor<F, L> {

  fun AF(): Applicative<F>

  override fun FF(): Functor<F> = AF()

  override fun <A> just(a: A): EitherT<F, L, A> =
    EitherT.just(AF(), a)

  override fun <A, B> EitherTOf<F, L, A>.map(f: (A) -> B): EitherT<F, L, B> =
    fix().map(AF(), f)

  override fun <A, B> EitherTOf<F, L, A>.ap(ff: EitherTOf<F, L, (A) -> B>): EitherT<F, L, B> =
    fix().ap(AF(), ff)
}

@extension
@undocumented
interface EitherTMonad<F, L> : Monad<EitherTPartialOf<F, L>>, EitherTApplicative<F, L> {

  fun MF(): Monad<F>

  override fun AF(): Applicative<F> = MF()

  override fun <A, B> EitherTOf<F, L, A>.map(f: (A) -> B): EitherT<F, L, B> =
    fix().map(MF(), f)

  override fun <A, B> EitherTOf<F, L, A>.ap(ff: EitherTOf<F, L, (A) -> B>): EitherT<F, L, B> =
    fix().ap(MF(), ff)

  override fun <A, B> EitherTOf<F, L, A>.flatMap(f: (A) -> EitherTOf<F, L, B>): EitherT<F, L, B> =
    fix().flatMap(MF(), f)

  override fun <A, B> tailRecM(a: A, f: (A) -> EitherTOf<F, L, Either<A, B>>): EitherT<F, L, B> =
    EitherT.tailRecM(MF(), a, f)
}

@extension
@undocumented
interface EitherTApplicativeError<F, L> : ApplicativeError<EitherTPartialOf<F, L>, L>, EitherTApplicative<F, L> {

  fun AE(): ApplicativeError<F, L>

  override fun AF(): Applicative<F> = AE()

  override fun <A> EitherTOf<F, L, A>.handleErrorWith(f: (L) -> EitherTOf<F, L, A>): EitherT<F, L, A> = AE().run {
    EitherT(value().handleErrorWith { l -> f(l).value() })
  }

  override fun <A> raiseError(e: L): EitherT<F, L, A> = AE().run {
    EitherT.liftF(this, raiseError(e))
  }

}

@extension
@undocumented
interface EitherTMonadError<F, L> : MonadError<EitherTPartialOf<F, L>, L>, EitherTApplicativeError<F, L>, EitherTMonad<F, L> {
  override fun MF(): Monad<F>
  override fun AE(): ApplicativeError<F, L>
  override fun AF(): Applicative<F> = MF()
}

fun <F, L> EitherT.Companion.monadError(ME: MonadError<F, L>): MonadError<EitherTPartialOf<F, L>, L> =
  object : EitherTMonadError<F, L> {
    override fun MF(): Monad<F> = ME
    override fun AE(): ApplicativeError<F, L> = ME
  }

@extension
@undocumented
interface EitherTMonadThrow<F> : MonadThrow<EitherTPartialOf<F, Throwable>>, EitherTMonadError<F, Throwable> {
  override fun MF(): Monad<F>
  override fun AE(): ApplicativeError<F, Throwable>
}

@extension
@undocumented
interface EitherTFoldable<F, L> : Foldable<EitherTPartialOf<F, L>> {

  fun FFF(): Foldable<F>

  override fun <B, C> EitherTOf<F, L, B>.foldLeft(b: C, f: (C, B) -> C): C =
    fix().foldLeft(FFF(), b, f)

  override fun <B, C> EitherTOf<F, L, B>.foldRight(lb: Eval<C>, f: (B, Eval<C>) -> Eval<C>): Eval<C> =
    fix().foldRight(FFF(), lb, f)
}

@extension
@undocumented
interface EitherTTraverse<F, L> : Traverse<EitherTPartialOf<F, L>>, EitherTFunctor<F, L>, EitherTFoldable<F, L> {

  fun TF(): Traverse<F>

  override fun FF(): Functor<F> = TF()

  override fun FFF(): Foldable<F> = TF()

  override fun <A, B> EitherTOf<F, L, A>.map(f: (A) -> B): EitherT<F, L, B> =
    fix().map(TF(), f)

  override fun <G, B, C> EitherTOf<F, L, B>.traverse(AP: Applicative<G>, f: (B) -> Kind<G, C>): Kind<G, EitherT<F, L, C>> =
    fix().traverse(TF(), AP, f)
}

@extension
@undocumented
interface EitherTSemigroupK<F, L> : SemigroupK<EitherTPartialOf<F, L>> {
  fun MF(): Monad<F>

  override fun <A> EitherTOf<F, L, A>.combineK(y: EitherTOf<F, L, A>): EitherT<F, L, A> =
    fix().combineK(MF(), y)
}

fun <F, A, B, C> EitherTOf<F, A, B>.foldLeft(FF: Foldable<F>, b: C, f: (C, B) -> C): C =
  FF.compose(Either.foldable<A>()).foldLC(value(), b, f)

fun <F, A, B, C> EitherTOf<F, A, B>.foldRight(FF: Foldable<F>, lb: Eval<C>, f: (B, Eval<C>) -> Eval<C>): Eval<C> = FF.compose(Either.foldable<A>()).run {
  value().foldRC(lb, f)
}

fun <F, A, B, G, C> EitherTOf<F, A, B>.traverse(FF: Traverse<F>, GA: Applicative<G>, f: (B) -> Kind<G, C>): Kind<G, EitherT<F, A, C>> {
  val fa: Kind<G, Kind<Nested<F, EitherPartialOf<A>>, C>> = ComposedTraverse(FF, Either.traverse(), Either.monad<A>()).run { value().traverseC(f, GA) }
  val mapper: (Kind<Nested<F, EitherPartialOf<A>>, C>) -> EitherT<F, A, C> = { nested -> EitherT(FF.run { nested.unnest().map { it.fix() } }) }
  return GA.run { fa.map(mapper) }
}

fun <F, G, A, B> EitherTOf<F, A, Kind<G, B>>.sequence(FF: Traverse<F>, GA: Applicative<G>): Kind<G, EitherT<F, A, B>> =
  traverse(FF, GA, ::identity)

fun <F, L> EitherT.Companion.applicativeError(MF: Monad<F>): ApplicativeError<EitherTPartialOf<F, L>, L> =
  object : ApplicativeError<EitherTPartialOf<F, L>, L>, EitherTApplicative<F, L> {

    override fun AF(): Applicative<F> = MF

    override fun <A> raiseError(e: L): EitherTOf<F, L, A> =
      EitherT(MF.just(Left(e)))

    override fun <A> EitherTOf<F, L, A>.handleErrorWith(f: (L) -> EitherTOf<F, L, A>): EitherT<F, L, A> =
      handleErrorWith(this, f, MF)
  }

fun <F, L> EitherT.Companion.monadError(MF: Monad<F>): MonadError<EitherTPartialOf<F, L>, L> =
  object : MonadError<EitherTPartialOf<F, L>, L>, EitherTMonad<F, L> {
    override fun MF(): Monad<F> = MF

    override fun <A> raiseError(e: L): EitherTOf<F, L, A> =
      EitherT(MF.just(Left(e)))

    override fun <A> EitherTOf<F, L, A>.handleErrorWith(f: (L) -> EitherTOf<F, L, A>): EitherT<F, L, A> =
      handleErrorWith(this, f, MF())
  }

private fun <F, L, A> handleErrorWith(fa: EitherTOf<F, L, A>, f: (L) -> EitherTOf<F, L, A>, MF: Monad<F>): EitherT<F, L, A> =
  MF.run {
    EitherT(fa.value().flatMap {
      when (it) {
        is Either.Left -> f(it.a).value()
        is Either.Right -> just(it)
      }
    })
  }

@extension
@undocumented
interface EitherTFx<F> : arrow.typeclasses.suspended.monaderror.Fx<EitherTPartialOf<F, Throwable>> {

  fun M(): MonadThrow<F>

  override fun monadError(): MonadThrow<EitherTPartialOf<F, Throwable>> =
    EitherT.monadThrow(M(), M())

}