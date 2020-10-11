package arrow.ui

import arrow.typeclasses.Comonad

typealias ForMooreInput = ForCo

typealias MooreInputPartialOf<I> = CoPartialOf<MoorePartialOf<I>>

typealias MooreInputOf<I, A> = CoOf<I, A>

typealias MooreInput<I, A> = Co<MoorePartialOf<I>, A>

fun <I, A, J> MooreInput<I, A>.mapInput(CMW: Comonad<MoorePartialOf<J>>, f: (I) -> J): MooreInput<J, A> =
  MooreInput<J, A>(CMW) { moore -> cow(moore.fix().contramapInput(f)) }

fun MooreInput() = MooreInputApi

object MooreInputApi {
  fun <I, A> liftInput(CMW: Comonad<MoorePartialOf<I>>, input: I, a: A): MooreInput<I, A> =
    MooreInput(CMW) { moore -> moore.fix().handle(input).extract()(a) }

  fun <I> from(CMW: Comonad<MoorePartialOf<I>>, input: I): MooreInput<I, Unit> =
    liftInput(CMW, input, Unit)
}
