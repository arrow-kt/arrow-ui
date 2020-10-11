package arrow.ui.extensions

import arrow.core.ForId
import arrow.core.Id
import arrow.core.extensions.id.functor.functor
import arrow.core.fix
import arrow.mtl.StatePartialOf
import arrow.mtl.StateTPartialOf
import arrow.mtl.fix
import arrow.mtl.run
import arrow.typeclasses.Comonad
import arrow.typeclasses.Functor
import arrow.ui.Co
import arrow.ui.CoPartialOf
import arrow.ui.MooreActionPartialOf
import arrow.ui.MoorePartialOf
import arrow.ui.Pairing
import arrow.ui.Store
import arrow.ui.StorePartialOf
import arrow.ui.StoreT
import arrow.ui.StoreTPartialOf
import arrow.ui.fix
import arrow.ui.run

/**
 * Provides a Pairing for [StateT] - [StoreT]
 */
fun <S, F, G> Pairing.Companion.pairStateTStoreT(
  FF: Functor<F>,
  FG: Functor<G>,
  pairing: Pairing<F, G>
): Pairing<StateTPartialOf<S, F>, StoreTPartialOf<S, G>> =
  Pairing { state, store, f ->
    pairing.pair(FF, FG, state.fix().run(store.fix().state), store.fix().render) { a, b ->
      f(a.b, b(a.a))
    }
  }

/**
 * Provides a Pairing for [State] - [Store]
 */
fun <S> Pairing.Companion.pairStateStore(): Pairing<StatePartialOf<S>, StorePartialOf<S>> =
  pairStateTStoreT(Id.functor(), Id.functor(), pairId())

/**
 * Provides a Pairing for [MooreAction] - [Moore]
 */
fun <I> Pairing.Companion.pairActionMoore(CMW: Comonad<MoorePartialOf<I>>): Pairing<MooreActionPartialOf<I>, MoorePartialOf<I>> =
  Pairing { input, moore, f ->
    CMW.pairing(CMW).pairFlipped(CMW, Co().functor(), input, moore, f)
  }

private fun pairId(): Pairing<ForId, ForId> = Pairing(Id.functor()) { fa, gb ->
  fa.fix().extract()(gb.fix().extract())
}

/**
 * Provides a [Pairing] for this [Comonad] and its dual [Monad]
 */
fun <F> Comonad<F>.pairing(FF: Functor<F>): Pairing<F, CoPartialOf<F>> =
  Pairing(FF) { wab, cowa -> cowa.fix().run(wab) }
