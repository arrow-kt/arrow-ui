package arrow.ui

import arrow.Kind
import arrow.core.ForId
import arrow.core.Id
import arrow.core.curry
import arrow.core.extensions.id.functor.functor
import arrow.core.fix
import arrow.higherkind
import arrow.mtl.StatePartialOf
import arrow.mtl.StateTPartialOf
import arrow.mtl.fix
import arrow.mtl.run
import arrow.typeclasses.Comonad
import arrow.typeclasses.Functor

// forall a b c. f a -> g b -> (a -> b -> c) -> c

// (Kind<F, A>, Kind<G, B>, (A, B) -> C) -> C
typealias PairingFun<F, G> = (Kind<F, Any>, Kind<G, Any>, (Any, Any) -> Any) -> Any

/**
 * `Pairing` represents a relationship between [Functor]s `F` and `G`, where the sums in one can annihilate
 * the products in the other.
 */
@higherkind
class Pairing<F, G>(private val pairing: PairingFun<F, G>) : PairingOf<F, G>, PairingKindedJ<F, G> {

  /**
   * Annihilate the [F] and [G] effects by calling the wrapped function in [F] with the
   * wrapped value.
   *
   * @param FF [Functor] for context [F]
   * @param FG [Functor] for context [G]
   * @param fab a [F]-effectful `([A]) -> [B]`
   * @param ga a [G]-effectful `[A]`
   */
  fun <A, B> zap(FF: Functor<F>, FG: Functor<G>, fab: Kind<F, (A) -> B>, ga: Kind<G, A>): B =
    pair(FF, FG, fab, ga) { f, a -> f(a) }

  /**
   * Annihilate the [F] and [G] effects by extracting the values from their contexts and
   * using the combination function
   *
   * @param FF [Functor] for context [F]
   * @param FG [Functor] for context [G]
   * @param fa a [F]-effectful value [A]
   * @param gb a [G]-effectful value [B]
   * @param f combination function
   */
  fun <A, B, C> pair(FF: Functor<F>, FG: Functor<G>, fa: Kind<F, A>, gb: Kind<G, B>, f: (A, B) -> C): C =
    FF.run {
      FG.run {
        pairing(
          fa.map { a -> a as Any },
          gb.map { b -> b as Any }
        ) { a, b -> f(a as A, b as B) as Any } as C
      }
    }

  /**
   * Explores the space given by one [Functor], using the other as an explorer
   *
   * @param FF [Functor] for context [F]
   * @param FG [Functor] for context [G]
   * @param fa Explorer functorial value
   * @param ggb Space functorial value
   */
  fun <A, B> select(FF: Functor<F>, FG: Functor<G>, fa: Kind<F, A>, ggb: Kind<G, Kind<G, B>>): Kind<G, B> =
    pair(FF, FG, fa, ggb) { _, gb -> gb }

  /**
   * Annihilates the [F] and [G] effectful values with arguments flipped
   *
   * @param FF [Functor] for context [F]
   * @param FG [Functor] for context [G]
   * @param ga a [G]-effectful value [A]
   * @param fb a [F]-effectful value [B]
   * @param f combination function
   */
  fun <A, B, C> pairFlipped(FF: Functor<F>, FG: Functor<G>, ga: Kind<G, A>, fb: Kind<F, B>, f: (A, B) -> C): C =
    pair(FF, FG, fb, ga) { b, a -> f(a, b) }

  companion object {
    operator fun <F, G> invoke(FF: Functor<F>, zap: (Kind<F, (Any) -> Any>, Kind<G, Any>) -> Any): Pairing<F, G> =
      FF.run { Pairing { fa, gb, fab -> zap(fa.map(fab.curry()), gb) } }
  }
}
