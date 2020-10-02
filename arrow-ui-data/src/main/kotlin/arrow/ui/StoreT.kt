package arrow.ui

import arrow.Kind
import arrow.core.andThen
import arrow.core.constant
import arrow.core.identity
import arrow.higherkind
import arrow.typeclasses.Applicative
import arrow.typeclasses.Comonad
import arrow.typeclasses.Functor
import arrow.typeclasses.Monoid

@higherkind
class StoreT<S, W, A>(
  val state: S,
  val render: Kind<W, (S) -> A>
) : StoreTOf<S, W, A>, StoreTKindedJ<S, W, A> {

  fun <B> map(FW: Functor<W>, f: (A) -> B): StoreT<S, W, B> = with(FW) {
    StoreT(state, render.map { ff -> ff.andThen(f) })
  }

  fun <B> ap(AW: Applicative<W>, MS: Monoid<S>, ff: StoreT<S, W, (A) -> B>): StoreT<S, W, B> = with(MS) {
    StoreT(ff.state.combine(state), AW.mapN(ff.render, render) { (rf, ra) -> { s -> rf(s)(ra(s)) } })
  }

  fun <B> coflatMap(CMW: Comonad<W>, f: (StoreT<S, W, A>) -> B): StoreT<S, W, B> = with(CMW) {
    StoreT(state, render.coflatMap { wa -> { s -> f(StoreT(s, wa)) } })
  }

  fun extract(CMW: Comonad<W>): A = with(CMW) { render.extract()(state) }

  fun duplicate(CMW: Comonad<W>): StoreT<S, W, StoreT<S, W, A>> = coflatMap(CMW, ::identity)

  fun position(): S = state

  fun peek(CMW: Comonad<W>, state: S): A = with(CMW) { render.extract()(state) }

  fun lower(FW: Functor<W>): Kind<W, A> = with(FW) { render.map { f -> f(state) } }

  fun move(CMW: Comonad<W>, newState: S): StoreT<S, W, A> = duplicate(CMW).peek(CMW, newState)

  companion object {
    fun <S, W, A> just(AW: Applicative<W>, MS: Monoid<S>, a: A): StoreT<S, W, A> = with(MS) {
      StoreT(empty(), AW.just(constant(a)))
    }

    fun <S, W> functor(FW: Functor<W>): Functor<StoreTPartialOf<S, W>> = object : Functor<StoreTPartialOf<S, W>> {
      override fun <A, B> Kind<StoreTPartialOf<S, W>, A>.map(f: (A) -> B): StoreT<S, W, B> =
        fix().map(FW, f)
    }
  }
}
