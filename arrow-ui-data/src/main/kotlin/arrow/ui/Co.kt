package arrow.ui

import arrow.Kind
import arrow.core.ForId
import arrow.core.Id
import arrow.core.andThen
import arrow.core.fix
import arrow.typeclasses.Functor

typealias ForCo = ForCoT

typealias CoOf<W, A> = CoTOf<W, ForId, A>

typealias CoPartialOf<W> = CoTPartialOf<W, ForId>

/**
 * Alias for [Co]
 */
typealias Transition<W, A> = Co<W, A>

/**
 * `Co` is equivalent to [CoT] where the base monad is [Id]
 */
typealias Co<W, A> = CoT<W, ForId, A>

/**
 * Runs the inner function
 *
 * @param w parameter for the inner function
 */
fun <W, A, R> Co<W, A>.run(w: Kind<W, (A) -> R>): R =
  runT(w.map { f -> f.andThen { Id.just(it) } }).fix().extract()

/**
 * Explores the space of a [Comonad] with a given [Monad]
 *
 * @param co Monadic actions to explore the [Comonad]
 * @param wa Comonadic space to explore
 */
fun <W, A, B> Co<W, A>.select(co: Co<W, (A) -> B>, wa: Kind<W, A>): Kind<W, B> =
  co.run(wa.coflatMap { wa -> { f -> wa.map(f) } })

fun Co() = CoApi

object CoApi {
  /**
   * Obtains a [Pairing] between a [Comonad] and its dual [Monad]
   *
   * @param FF [Functor] for [Comonad] [W]
   */
  fun <W> pair(FF: Functor<W>): Pairing<W, CoPartialOf<W>> =
    Pairing(FF) { wab, cowa -> cowa.fix().run(wab) }
}
