package arrow.data.extensions

import arrow.Kind
import arrow.core.Either
import arrow.core.Eval
import arrow.core.Tuple2
import arrow.data.ForSequenceK
import arrow.data.SequenceK
import arrow.data.SequenceKOf
import arrow.data.k
import arrow.data.extensions.sequencek.monad.map
import arrow.data.extensions.sequencek.monad.monad
import arrow.data.fix
import arrow.extension
import arrow.typeclasses.Applicative
import arrow.typeclasses.Apply
import arrow.typeclasses.Eq
import arrow.typeclasses.Foldable
import arrow.typeclasses.Functor
import arrow.typeclasses.Hash
import arrow.typeclasses.Monad
import arrow.typeclasses.Monoid
import arrow.typeclasses.MonoidK
import arrow.typeclasses.Monoidal
import arrow.typeclasses.Semigroup
import arrow.typeclasses.SemigroupK
import arrow.typeclasses.Semigroupal
import arrow.typeclasses.Show
import arrow.typeclasses.Traverse
import arrow.typeclasses.fix
import arrow.typeclasses.suspended.monad.Fx
import arrow.data.combineK as sequenceCombineK

@extension
interface SequenceKSemigroup<A> : Semigroup<SequenceK<A>> {
  override fun SequenceK<A>.combine(b: SequenceK<A>): SequenceK<A> = (this.sequence + b.sequence).k()
}

@extension
interface SequenceKSemigroupal : Semigroupal<ForSequenceK> {
  override fun <A, B> Kind<ForSequenceK, A>.product(fb: Kind<ForSequenceK, B>): Kind<ForSequenceK, Tuple2<A, B>> =
    fb.fix().ap(this.map { a: A -> { b: B -> Tuple2(a, b) } })
}

@extension
interface SequenceKMonoidal : Monoidal<ForSequenceK>, SequenceKSemigroupal {
  override fun <A> identity(): Kind<ForSequenceK, A> = SequenceK.empty()
}

@extension
interface SequenceKMonoid<A> : Monoid<SequenceK<A>> {
  override fun SequenceK<A>.combine(b: SequenceK<A>): SequenceK<A> = (this.sequence + b.sequence).k()

  override fun empty(): SequenceK<A> = emptySequence<A>().k()
}

@extension
interface SequenceKEq<A> : Eq<SequenceK<A>> {

  fun EQ(): Eq<A>

  override fun SequenceK<A>.eqv(b: SequenceK<A>): Boolean =
    zip(b) { aa, bb -> EQ().run { aa.eqv(bb) } }.fold(true) { acc, bool ->
      acc && bool
    }
}

@extension
interface SequenceKShow<A> : Show<SequenceK<A>> {
  override fun SequenceK<A>.show(): String =
    toString()
}

@extension
interface SequenceKFunctor : Functor<ForSequenceK> {
  override fun <A, B> Kind<ForSequenceK, A>.map(f: (A) -> B): SequenceK<B> =
    fix().map(f)
}

@extension
interface SequenceKApply : Apply<ForSequenceK> {
  override fun <A, B> Kind<ForSequenceK, A>.ap(ff: Kind<ForSequenceK, (A) -> B>): SequenceK<B> =
    fix().ap(ff)

  override fun <A, B> Kind<ForSequenceK, A>.map(f: (A) -> B): SequenceK<B> =
    fix().map(f)

  override fun <A, B, Z> Kind<ForSequenceK, A>.map2(fb: Kind<ForSequenceK, B>, f: (Tuple2<A, B>) -> Z): SequenceK<Z> =
    fix().map2(fb, f)
}

@extension
interface SequenceKApplicative : Applicative<ForSequenceK> {
  override fun <A, B> Kind<ForSequenceK, A>.ap(ff: Kind<ForSequenceK, (A) -> B>): SequenceK<B> =
    fix().ap(ff)

  override fun <A, B> Kind<ForSequenceK, A>.map(f: (A) -> B): SequenceK<B> =
    fix().map(f)

  override fun <A, B, Z> Kind<ForSequenceK, A>.map2(fb: Kind<ForSequenceK, B>, f: (Tuple2<A, B>) -> Z): SequenceK<Z> =
    fix().map2(fb, f)

  override fun <A> just(a: A): SequenceK<A> =
    SequenceK.just(a)
}

@extension
interface SequenceKMonad : Monad<ForSequenceK> {
  override fun <A, B> Kind<ForSequenceK, A>.ap(ff: Kind<ForSequenceK, (A) -> B>): SequenceK<B> =
    fix().ap(ff)

  override fun <A, B> Kind<ForSequenceK, A>.flatMap(f: (A) -> Kind<ForSequenceK, B>): SequenceK<B> =
    fix().flatMap(f)

  override fun <A, B> tailRecM(a: A, f: kotlin.Function1<A, SequenceKOf<Either<A, B>>>): SequenceK<B> =
    SequenceK.tailRecM(a, f)

  override fun <A, B> Kind<ForSequenceK, A>.map(f: (A) -> B): SequenceK<B> =
    fix().map(f)

  override fun <A, B, Z> Kind<ForSequenceK, A>.map2(fb: Kind<ForSequenceK, B>, f: (Tuple2<A, B>) -> Z): SequenceK<Z> =
    fix().map2(fb, f)

  override fun <A> just(a: A): SequenceK<A> =
    SequenceK.just(a)
}

@extension
interface SequenceKFoldable : Foldable<ForSequenceK> {
  override fun <A, B> Kind<ForSequenceK, A>.foldLeft(b: B, f: (B, A) -> B): B =
    fix().foldLeft(b, f)

  override fun <A, B> Kind<ForSequenceK, A>.foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> =
    fix().foldRight(lb, f)
}

@extension
interface SequenceKTraverse : Traverse<ForSequenceK> {
  override fun <A, B> Kind<ForSequenceK, A>.map(f: (A) -> B): SequenceK<B> =
    fix().map(f)

  override fun <G, A, B> Kind<ForSequenceK, A>.traverse(AP: Applicative<G>, f: (A) -> Kind<G, B>): Kind<G, SequenceK<B>> =
    fix().traverse(AP, f)

  override fun <A, B> Kind<ForSequenceK, A>.foldLeft(b: B, f: (B, A) -> B): B =
    fix().foldLeft(b, f)

  override fun <A, B> Kind<ForSequenceK, A>.foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> =
    fix().foldRight(lb, f)
}

@extension
interface SequenceKSemigroupK : SemigroupK<ForSequenceK> {
  override fun <A> Kind<ForSequenceK, A>.combineK(y: Kind<ForSequenceK, A>): SequenceK<A> =
    fix().sequenceCombineK(y)
}

@extension
interface SequenceKMonoidK : MonoidK<ForSequenceK> {
  override fun <A> empty(): SequenceK<A> =
    SequenceK.empty()

  override fun <A> Kind<ForSequenceK, A>.combineK(y: Kind<ForSequenceK, A>): SequenceK<A> =
    fix().sequenceCombineK(y)
}

@extension
interface SequenceKHash<A> : Hash<SequenceK<A>>, SequenceKEq<A> {
  fun HA(): Hash<A>

  override fun EQ(): Eq<A> = HA()

  override fun SequenceK<A>.hash(): Int = foldLeft(1) { hash, a ->
    31 * hash + HA().run { a.hash() }
  }
}

@extension
interface SequenceKFx<F> : Fx<ForSequenceK> {

  override fun monad(): Monad<ForSequenceK> =
    SequenceK.monad()
}
