package arrow.ui

import arrow.typeclasses.Comonad

typealias ForMooreAction = ForCo

typealias MooreActionPartialOf<I> = CoPartialOf<MoorePartialOf<I>>

typealias MooreActionOf<I, A> = CoOf<I, A>

/**
 * MooreAction is the dual [Pairing] [Monad] of [Moore], obtained automatically using the [Co] type
 */
typealias MooreAction<I, A> = Co<MoorePartialOf<I>, A>

/**
 * Transform the input of a MooreAction
 *
 * @param CMW [Comonad] for [Moore]
 * @param f Transforming function
 */
fun <I, A, J> MooreAction<I, A>.mapInput(CMW: Comonad<MoorePartialOf<J>>, f: (I) -> J): MooreAction<J, A> =
  MooreAction<J, A>(CMW) { moore -> cow(moore.fix().contramapInput(f)) }

fun MooreAction() = MooreActionApi

object MooreActionApi {
  /**
   * Creates a [MooreAction] from an input and a value
   *
   * @param CWM [Comonad] for [Moore]
   * @param input Input
   * @param a Value
   */
  fun <I, A> liftInput(CMW: Comonad<MoorePartialOf<I>>, input: I, a: A): MooreAction<I, A> =
    MooreAction(CMW) { moore -> moore.fix().handle(input).extract()(a) }

  /**
   * Creates a [MooreAction] from an input
   *
   * @param CMW [Comonad] for [Moore]
   * @param input Input
   */
  fun <I> from(CMW: Comonad<MoorePartialOf<I>>, input: I): MooreAction<I, Unit> =
    liftInput(CMW, input, Unit)
}
