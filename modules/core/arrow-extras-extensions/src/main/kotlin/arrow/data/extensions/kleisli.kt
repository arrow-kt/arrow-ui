package arrow.data.extensions

import arrow.Kind
import arrow.core.Either
import arrow.core.Id
import arrow.core.Tuple2
import arrow.data.Kleisli
import arrow.data.ReaderApi
import arrow.data.ReaderPartialOf
import arrow.extension
import arrow.core.extensions.id.applicative.applicative
import arrow.core.extensions.id.functor.functor
import arrow.core.extensions.id.monad.monad
import arrow.data.ForKleisli
import arrow.core.Ior
import arrow.data.KleisliOf
import arrow.data.KleisliPartialOf
import arrow.data.extensions.kleisli.applicative.applicative
import arrow.data.extensions.kleisli.functor.functor
import arrow.data.extensions.kleisli.monad.monad
import arrow.data.fix
import arrow.data.run
import arrow.typeclasses.Applicative
import arrow.typeclasses.ApplicativeError
import arrow.typeclasses.Apply
import arrow.typeclasses.Conested
import arrow.typeclasses.Contravariant
import arrow.typeclasses.Decidable
import arrow.typeclasses.Divide
import arrow.typeclasses.Divisible
import arrow.typeclasses.Functor
import arrow.typeclasses.Monad
import arrow.typeclasses.MonadSyntax
import arrow.typeclasses.MonadError
import arrow.typeclasses.MonadThrow
import arrow.typeclasses.conest
import arrow.typeclasses.counnest
import arrow.undocumented

@extension
interface KleisliFunctor<F, D> : Functor<KleisliPartialOf<F, D>> {

  fun FF(): Functor<F>

  override fun <A, B> KleisliOf<F, D, A>.map(f: (A) -> B): Kleisli<F, D, B> =
    fix().map(FF(), f)
}

@extension
interface KleisliContravariant<F, D> : Contravariant<Conested<Kind<ForKleisli, F>, D>> {
  override fun <A, B> Kind<Conested<Kind<ForKleisli, F>, D>, A>.contramap(f: (B) -> A): Kind<Conested<Kind<ForKleisli, F>, D>, B> =
    counnest().fix().local(f).conest()

  fun <A, B> KleisliOf<F, A, D>.contramapC(f: (B) -> A): KleisliOf<F, B, D> =
    conest().contramap(f).counnest()
}

@extension
interface KleisliContravariantInstance<F, D> : Contravariant<KleisliPartialOf<F, D>> {

  fun CF(): Contravariant<F>

  override fun <A, B> Kind<KleisliPartialOf<F, D>, A>.contramap(f: (B) -> A): Kind<KleisliPartialOf<F, D>, B> =
    Kleisli { d -> CF().run { fix().run(d).contramap(f) } }
}

@extension
interface KleisliDivideInstance<F, D> : Divide<KleisliPartialOf<F, D>>, KleisliContravariantInstance<F, D> {

  fun DF(): Divide<F>
  override fun CF(): Contravariant<F> = DF()

  override fun <A, B, Z> divide(fa: Kind<KleisliPartialOf<F, D>, A>, fb: Kind<KleisliPartialOf<F, D>, B>, f: (Z) -> Tuple2<A, B>): Kind<KleisliPartialOf<F, D>, Z> =
    Kleisli { d -> DF().divide(fa.fix().run(d), fb.fix().run(d), f) }
}

@extension
interface KleisliDivisibleInstance<F, D> : Divisible<KleisliPartialOf<F, D>>, KleisliDivideInstance<F, D> {

  fun DFF(): Divisible<F>
  override fun DF(): Divide<F> = DFF()

  override fun <A> conquer(): Kind<KleisliPartialOf<F, D>, A> =
    Kleisli { DFF().conquer() }
}

@extension
interface KleisliDecidableInstance<F, D> : Decidable<KleisliPartialOf<F, D>>, KleisliDivisibleInstance<F, D> {

  fun DFFF(): Decidable<F>
  override fun DFF(): Divisible<F> = DFFF()

  override fun <A, B, Z> choose(fa: Kind<KleisliPartialOf<F, D>, A>, fb: Kind<KleisliPartialOf<F, D>, B>, f: (Z) -> Either<A, B>): Kind<KleisliPartialOf<F, D>, Z> =
    Kleisli { d -> DFFF().choose(fa.fix().run(d), fb.fix().run(d), f) }
}

@extension
interface KleisliApply<F, D> : Apply<KleisliPartialOf<F, D>>, KleisliFunctor<F, D> {

  fun AF(): Applicative<F>

  override fun FF(): Functor<F> = AF()

  override fun <A, B> KleisliOf<F, D, A>.map(f: (A) -> B): Kleisli<F, D, B> =
    fix().map(AF(), f)

  override fun <A, B> KleisliOf<F, D, A>.ap(ff: KleisliOf<F, D, (A) -> B>): Kleisli<F, D, B> =
    fix().ap(AF(), ff)

  override fun <A, B> KleisliOf<F, D, A>.product(fb: KleisliOf<F, D, B>): Kleisli<F, D, Tuple2<A, B>> =
    Kleisli { AF().run { run(it).product(fb.run(it)) } }
}

@extension
interface KleisliApplicative<F, D> : Applicative<KleisliPartialOf<F, D>>, KleisliFunctor<F, D> {

  fun AF(): Applicative<F>

  override fun FF(): Functor<F> = AF()

  override fun <A> just(a: A): Kleisli<F, D, A> =
    Kleisli { AF().just(a) }

  override fun <A, B> KleisliOf<F, D, A>.map(f: (A) -> B): Kleisli<F, D, B> =
    fix().map(AF(), f)

  override fun <A, B> KleisliOf<F, D, A>.ap(ff: KleisliOf<F, D, (A) -> B>): Kleisli<F, D, B> =
    fix().ap(AF(), ff)

  override fun <A, B> KleisliOf<F, D, A>.product(fb: KleisliOf<F, D, B>): Kleisli<F, D, Tuple2<A, B>> =
    Kleisli { AF().run { run(it).product(fb.run(it)) } }
}

@extension
interface KleisliMonad<F, D> : Monad<KleisliPartialOf<F, D>>, KleisliApplicative<F, D> {

  fun MF(): Monad<F>

  override fun AF(): Applicative<F> = MF()

  override fun <A, B> KleisliOf<F, D, A>.map(f: (A) -> B): Kleisli<F, D, B> =
    fix().map(MF(), f)

  override fun <A, B> KleisliOf<F, D, A>.flatMap(f: (A) -> KleisliOf<F, D, B>): Kleisli<F, D, B> =
    fix().flatMap(MF(), f)

  override fun <A, B> KleisliOf<F, D, A>.ap(ff: KleisliOf<F, D, (A) -> B>): Kleisli<F, D, B> =
    fix().ap(MF(), ff)

  override fun <A, B> tailRecM(a: A, f: (A) -> KleisliOf<F, D, Either<A, B>>): Kleisli<F, D, B> =
    Kleisli.tailRecM(MF(), a, f)
}

@extension
interface KleisliApplicativeError<F, D, E> : ApplicativeError<KleisliPartialOf<F, D>, E>, KleisliApplicative<F, D> {

  fun AE(): ApplicativeError<F, E>

  override fun AF(): Applicative<F> = AE()

  override fun <A> KleisliOf<F, D, A>.handleErrorWith(f: (E) -> KleisliOf<F, D, A>): Kleisli<F, D, A> =
    fix().handleErrorWith(AE(), f)

  override fun <A> raiseError(e: E): Kleisli<F, D, A> =
    Kleisli.raiseError(AE(), e)
}

@extension
interface KleisliMonadError<F, D, E> : MonadError<KleisliPartialOf<F, D>, E>, KleisliApplicativeError<F, D, E>, KleisliMonad<F, D> {

  fun ME(): MonadError<F, E>

  override fun MF(): Monad<F> = ME()

  override fun AE(): ApplicativeError<F, E> = ME()

  override fun AF(): Applicative<F> = ME()
}

@extension
@undocumented
interface KleisliMonadThrow<F, D> : MonadThrow<KleisliPartialOf<F, D>>, KleisliMonadError<F, D, Throwable> {
  override fun ME(): MonadError<F, Throwable>
}

/**
 * Alias for [Kleisli] for [Id]
 */
fun <D> ReaderApi.functor(): Functor<ReaderPartialOf<D>> = Kleisli.functor(Id.functor())

/**
 * Alias for [Kleisli] for [Id]
 */
fun <D> ReaderApi.applicative(): Applicative<ReaderPartialOf<D>> = Kleisli.applicative(Id.applicative())

/**
 * Alias for [Kleisli] for [Id]
 */
fun <D> ReaderApi.monad(): Monad<ReaderPartialOf<D>> = Kleisli.monad(Id.monad())

fun <F, D, A> Ior.Companion.fx(MF: Monad<F>, c: suspend MonadSyntax<KleisliPartialOf<F, D>>.() -> A): Kleisli<F, D, A> =
  Kleisli.monad<F, D>(MF).fx.monad(c).fix()
