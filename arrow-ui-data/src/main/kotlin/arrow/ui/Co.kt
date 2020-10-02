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

typealias Co<W, A> = CoT<W, ForId, A>

typealias Transition<W, A> = Co<W, A>

fun <W, A, B> Co<W, A>.map(f: (A) -> B): Co<W, B> = map(f)

fun <W, A, B> Co<W, A>.ap(ff: CoOf<W, (A) -> B>): Co<W, B> = ap(ff)

fun <W, A, B> Co<W, A>.flatMap(f: (A) -> Co<W, B>): Co<W, B> = flatMap(f)

fun <W, A, R> Co<W, A>.run(w: Kind<W, (A) -> R>): R =
  runT(w.map { f -> f.andThen { Id.just(it) } }).fix().extract()

fun <W, A, B> Co<W, A>.select(co: Co<W, (A) -> B>, wa: Kind<W, A>): Kind<W, B> =
  co.run(wa.coflatMap { wa -> { f -> wa.map(f) } })

fun Co() = CoApi

object CoApi {
  fun <W> pair(FF: Functor<W>): Pairing<W, CoPartialOf<W>> =
    Pairing(FF) { wab, cowa -> cowa.fix().run(wab) }
}
