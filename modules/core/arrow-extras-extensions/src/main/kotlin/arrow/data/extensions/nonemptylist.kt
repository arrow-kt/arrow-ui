package arrow.data.extensions

import arrow.Kind
import arrow.core.Either
import arrow.core.Eval
import arrow.data.ForNonEmptyList
import arrow.data.Nel
import arrow.data.NonEmptyList
import arrow.data.NonEmptyListOf
import arrow.data.extensions.nonemptylist.monad.monad
import arrow.data.fix
import arrow.extension
import arrow.typeclasses.Applicative
import arrow.typeclasses.Apply
import arrow.typeclasses.Bimonad
import arrow.typeclasses.Comonad
import arrow.typeclasses.Eq
import arrow.typeclasses.Foldable
import arrow.typeclasses.Functor
import arrow.typeclasses.Hash
import arrow.typeclasses.Monad
import arrow.typeclasses.Reducible
import arrow.typeclasses.Semigroup
import arrow.typeclasses.SemigroupK
import arrow.typeclasses.Show
import arrow.typeclasses.Traverse
import arrow.typeclasses.suspended.monad.Fx
import arrow.data.combineK as nelCombineK

@extension
interface NonEmptyListSemigroup<A> : Semigroup<NonEmptyList<A>> {
  override fun NonEmptyList<A>.combine(b: NonEmptyList<A>): NonEmptyList<A> = this + b
}

@extension
interface NonEmptyListEq<A> : Eq<NonEmptyList<A>> {

  fun EQ(): Eq<A>

  override fun NonEmptyList<A>.eqv(b: NonEmptyList<A>): Boolean =
    all.zip(b.all) { aa, bb -> EQ().run { aa.eqv(bb) } }.fold(true) { acc, bool ->
      acc && bool
    }
}

@extension
interface NonEmptyListShow<A> : Show<NonEmptyList<A>> {
  override fun NonEmptyList<A>.show(): String =
    toString()
}

@extension
interface NonEmptyListFunctor : Functor<ForNonEmptyList> {
  override fun <A, B> Kind<ForNonEmptyList, A>.map(f: (A) -> B): NonEmptyList<B> =
    fix().map(f)
}

@extension
interface NonEmptyListApply : Apply<ForNonEmptyList> {
  override fun <A, B> Kind<ForNonEmptyList, A>.ap(ff: Kind<ForNonEmptyList, (A) -> B>): NonEmptyList<B> =
    fix().ap(ff)

  override fun <A, B> Kind<ForNonEmptyList, A>.map(f: (A) -> B): NonEmptyList<B> =
    fix().map(f)
}

@extension
interface NonEmptyListApplicative : Applicative<ForNonEmptyList> {
  override fun <A, B> Kind<ForNonEmptyList, A>.ap(ff: Kind<ForNonEmptyList, (A) -> B>): NonEmptyList<B> =
    fix().ap(ff)

  override fun <A, B> Kind<ForNonEmptyList, A>.map(f: (A) -> B): NonEmptyList<B> =
    fix().map(f)

  override fun <A> just(a: A): NonEmptyList<A> =
    NonEmptyList.just(a)
}

@extension
interface NonEmptyListMonad : Monad<ForNonEmptyList> {
  override fun <A, B> Kind<ForNonEmptyList, A>.ap(ff: Kind<ForNonEmptyList, (A) -> B>): NonEmptyList<B> =
    fix().ap(ff)

  override fun <A, B> Kind<ForNonEmptyList, A>.flatMap(f: (A) -> Kind<ForNonEmptyList, B>): NonEmptyList<B> =
    fix().flatMap(f)

  override fun <A, B> tailRecM(a: A, f: kotlin.Function1<A, NonEmptyListOf<Either<A, B>>>): NonEmptyList<B> =
    NonEmptyList.tailRecM(a, f)

  override fun <A, B> Kind<ForNonEmptyList, A>.map(f: (A) -> B): NonEmptyList<B> =
    fix().map(f)

  override fun <A> just(a: A): NonEmptyList<A> =
    NonEmptyList.just(a)
}

@extension
interface NonEmptyListComonad : Comonad<ForNonEmptyList> {
  override fun <A, B> Kind<ForNonEmptyList, A>.coflatMap(f: (Kind<ForNonEmptyList, A>) -> B): NonEmptyList<B> =
    fix().coflatMap(f)

  override fun <A> Kind<ForNonEmptyList, A>.extract(): A =
    fix().extract()

  override fun <A, B> Kind<ForNonEmptyList, A>.map(f: (A) -> B): NonEmptyList<B> =
    fix().map(f)
}

@extension
interface NonEmptyListBimonad : Bimonad<ForNonEmptyList> {
  override fun <A, B> Kind<ForNonEmptyList, A>.ap(ff: Kind<ForNonEmptyList, (A) -> B>): NonEmptyList<B> =
    fix().ap(ff)

  override fun <A, B> Kind<ForNonEmptyList, A>.flatMap(f: (A) -> Kind<ForNonEmptyList, B>): NonEmptyList<B> =
    fix().flatMap(f)

  override fun <A, B> tailRecM(a: A, f: kotlin.Function1<A, NonEmptyListOf<Either<A, B>>>): NonEmptyList<B> =
    NonEmptyList.tailRecM(a, f)

  override fun <A, B> Kind<ForNonEmptyList, A>.map(f: (A) -> B): NonEmptyList<B> =
    fix().map(f)

  override fun <A> just(a: A): NonEmptyList<A> =
    NonEmptyList.just(a)

  override fun <A, B> Kind<ForNonEmptyList, A>.coflatMap(f: (Kind<ForNonEmptyList, A>) -> B): NonEmptyList<B> =
    fix().coflatMap(f)

  override fun <A> Kind<ForNonEmptyList, A>.extract(): A =
    fix().extract()
}

@extension
interface NonEmptyListFoldable : Foldable<ForNonEmptyList> {
  override fun <A, B> Kind<ForNonEmptyList, A>.foldLeft(b: B, f: (B, A) -> B): B =
    fix().foldLeft(b, f)

  override fun <A, B> arrow.Kind<arrow.data.ForNonEmptyList, A>.foldRight(lb: arrow.core.Eval<B>, f: (A, arrow.core.Eval<B>) -> arrow.core.Eval<B>): Eval<B> =
    fix().foldRight(lb, f)

  override fun <A> Kind<ForNonEmptyList, A>.isEmpty(): kotlin.Boolean =
    fix().isEmpty()
}

@extension
interface NonEmptyListTraverse : Traverse<ForNonEmptyList> {
  override fun <A, B> Kind<ForNonEmptyList, A>.map(f: (A) -> B): NonEmptyList<B> =
    fix().map(f)

  override fun <G, A, B> Kind<ForNonEmptyList, A>.traverse(AP: Applicative<G>, f: (A) -> Kind<G, B>): Kind<G, NonEmptyList<B>> =
    fix().traverse(AP, f)

  override fun <A, B> Kind<ForNonEmptyList, A>.foldLeft(b: B, f: (B, A) -> B): B =
    fix().foldLeft(b, f)

  override fun <A, B> arrow.Kind<arrow.data.ForNonEmptyList, A>.foldRight(lb: arrow.core.Eval<B>, f: (A, arrow.core.Eval<B>) -> arrow.core.Eval<B>): Eval<B> =
    fix().foldRight(lb, f)

  override fun <A> Kind<ForNonEmptyList, A>.isEmpty(): kotlin.Boolean =
    fix().isEmpty()
}

@extension
interface NonEmptyListSemigroupK : SemigroupK<ForNonEmptyList> {
  override fun <A> Kind<ForNonEmptyList, A>.combineK(y: Kind<ForNonEmptyList, A>): NonEmptyList<A> =
    fix().nelCombineK(y)
}

@extension
interface NonEmptyListHash<A> : Hash<NonEmptyList<A>>, NonEmptyListEq<A> {
  fun HA(): Hash<A>

  override fun EQ(): Eq<A> = HA()

  override fun NonEmptyList<A>.hash(): Int = foldLeft(1) { hash, a ->
    31 * hash + HA().run { a.hash() }
  }
}

fun <F, A> Reducible<F>.toNonEmptyList(fa: Kind<F, A>): NonEmptyList<A> =
  fa.reduceRightTo({ a -> NonEmptyList.of(a) }, { a, lnel ->
    lnel.map { nonEmptyList -> NonEmptyList(a, listOf(nonEmptyList.head) + nonEmptyList.tail) }
  }).value()

@extension
interface NelFx : Fx<ForNonEmptyList> {

  override fun monad(): Monad<ForNonEmptyList> =
    Nel.monad()
}
