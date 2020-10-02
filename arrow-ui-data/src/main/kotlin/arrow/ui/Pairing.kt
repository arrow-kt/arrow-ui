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

@higherkind
class Pairing<F, G>(private val pairing: PairingFun<F, G>) : PairingOf<F, G>, PairingKindedJ<F, G> {

  fun <A, B> zap(FF: Functor<F>, FG: Functor<G>, fab: Kind<F, (A) -> B>, ga: Kind<G, A>): B =
    pair(FF, FG, fab, ga) { f, a -> f(a) }

  fun <A, B, C> pair(FF: Functor<F>, FG: Functor<G>, fa: Kind<F, A>, gb: Kind<G, B>, f: (A, B) -> C): C =
    FF.run {
      FG.run {
        pairing(
          fa.map { a -> a as Any },
          gb.map { b -> b as Any }
        ) { a, b -> f(a as A, b as B) as Any } as C
      }
    }

  fun <A, B> select(FF: Functor<F>, FG: Functor<G>, fa: Kind<F, A>, ggb: Kind<G, Kind<G, B>>): Kind<G, B> =
    pair(FF, FG, fa, ggb) { _, gb -> gb }

  fun <A, B, C> pairFlipped(FF: Functor<F>, FG: Functor<G>, ga: Kind<G, A>, fb: Kind<F, B>, f: (A, B) -> C): C =
    pair(FF, FG, fb, ga) { b, a -> f(a, b) }

  companion object {
    operator fun <F, G> invoke(FF: Functor<F>, zap: (Kind<F, (Any) -> Any>, Kind<G, Any>) -> Any): Pairing<F, G> =
      FF.run { Pairing { fa, gb, fab -> zap(fa.map(fab.curry()), gb) } }

    fun <S, F, G> pairStateTStoreT(FF: Functor<F>, FG: Functor<G>, pairing: Pairing<F, G>): Pairing<StateTPartialOf<S, F>, StoreTPartialOf<S, G>> =
      Pairing { state, store, f ->
        pairing.pair(FF, FG, state.fix().run(store.fix().state), store.fix().render) { a, b ->
          f(a.b, b(a.a))
        }
      }

    fun <S> pairStateStore(): Pairing<StatePartialOf<S>, StorePartialOf<S>> =
      pairStateTStoreT(Id.functor(), Id.functor(), pairId())

    private fun pairId(): Pairing<ForId, ForId> = Pairing(Id.functor()) { fa, gb -> fa.fix().extract()(gb) }
  }
}

fun <F> Comonad<F>.pair(FF: Functor<F>): Pairing<F, CoPartialOf<F>> =
  Pairing(FF) { wab, cowa -> cowa.fix().run(wab) }
