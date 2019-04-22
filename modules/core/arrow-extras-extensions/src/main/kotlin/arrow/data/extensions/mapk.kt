package arrow.data.extensions

import arrow.Kind
import arrow.core.Eval
import arrow.data.MapK
import arrow.data.MapKOf
import arrow.data.MapKPartialOf
import arrow.data.SetK
import arrow.data.k
import arrow.extension
import arrow.data.extensions.setk.eq.eq
import arrow.data.extensions.setk.hash.hash
import arrow.data.fix
import arrow.data.updated
import arrow.typeclasses.Applicative
import arrow.typeclasses.Eq
import arrow.typeclasses.Foldable
import arrow.typeclasses.Functor
import arrow.typeclasses.Hash
import arrow.typeclasses.Monoid
import arrow.typeclasses.Semigroup
import arrow.typeclasses.Show
import arrow.typeclasses.Traverse
import arrow.undocumented

@extension
@undocumented
interface MapKFunctor<K> : Functor<MapKPartialOf<K>> {
  override fun <A, B> Kind<MapKPartialOf<K>, A>.map(f: (A) -> B): MapK<K, B> = fix().map(f)
}

@extension
@undocumented
interface MapKFoldable<K> : Foldable<MapKPartialOf<K>> {

  override fun <A, B> Kind<MapKPartialOf<K>, A>.foldLeft(b: B, f: (B, A) -> B): B = fix().foldLeft(b, f)

  override fun <A, B> Kind<MapKPartialOf<K>, A>.foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> =
    fix().foldRight(lb, f)
}

@extension
@undocumented
interface MapKTraverse<K> : Traverse<MapKPartialOf<K>>, MapKFoldable<K> {

  override fun <G, A, B> MapKOf<K, A>.traverse(AP: Applicative<G>, f: (A) -> Kind<G, B>): Kind<G, MapKOf<K, B>> =
    fix().traverse(AP, f)
}

@extension
interface MapKSemigroup<K, A> : Semigroup<MapK<K, A>> {

  fun SG(): Semigroup<A>

  override fun MapK<K, A>.combine(b: MapK<K, A>): MapK<K, A> = with(SG()) {
    if (fix().size < b.fix().size) fix().foldLeft<A>(b.fix()) { my, (k, b) -> my.updated(k, b.maybeCombine(my[k])) }
    else b.fix().foldLeft<A>(fix()) { my, (k, a) -> my.updated(k, a.maybeCombine(my[k])) }
  }
}

@extension
interface MapKMonoid<K, A> : Monoid<MapK<K, A>>, MapKSemigroup<K, A> {

  override fun SG(): Semigroup<A>

  override fun empty(): MapK<K, A> = emptyMap<K, A>().k()
}

@extension
interface MapKEq<K, A> : Eq<MapK<K, A>> {

  fun EQK(): Eq<K>

  fun EQA(): Eq<A>

  override fun MapK<K, A>.eqv(b: MapK<K, A>): Boolean =
    if (SetK.eq(EQK()).run { keys.k().eqv(b.keys.k()) }) {
      keys.map { key ->
        b[key]?.let {
          EQA().run { getValue(key).eqv(it) }
        } ?: false
      }.fold(true) { b1, b2 -> b1 && b2 }
    } else false
}

@extension
interface MapKShow<K, A> : Show<MapK<K, A>> {
  override fun MapK<K, A>.show(): String =
    toString()
}

@extension
interface MapKHash<K, A> : Hash<MapK<K, A>>, MapKEq<K, A> {
  fun HK(): Hash<K>
  fun HA(): Hash<A>

  override fun EQK(): Eq<K> = HK()
  override fun EQA(): Eq<A> = HA()

  // Somewhat mirrors HashMap.Node.hashCode in that the combinator there between key and value is xor
  override fun MapK<K, A>.hash(): Int =
    SetK.hash(HK()).run { keys.k().hash() } xor foldLeft(1) { hash, a ->
      31 * hash + HA().run { a.hash() }
    }
}
