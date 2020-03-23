package arrow.ui.extensions

import arrow.Kind
import arrow.Proof
import arrow.TypeProof
import arrow.ui.Day
import arrow.extension
import arrow.typeclasses.Applicative
import arrow.typeclasses.Apply
import arrow.typeclasses.Comonad
import arrow.typeclasses.Functor
import arrow.ui.DayOf
import arrow.ui.DayPartialOf
import arrow.ui.fix
import arrow.undocumented
import arrowx.given


@undocumented
interface DayComonad<F, G> : Comonad<DayPartialOf<F, G>> {
  fun CF(): Comonad<F>

  fun CG(): Comonad<G>

  override fun <A, B> DayOf<F, G, A>.coflatMap(f: (DayOf<F, G, A>) -> B): Day<F, G, B> =
    fix().coflatMapLazy(CF(), CG(), f)

  override fun <A> DayOf<F, G, A>.extract(): A =
    fix().extract(CF(), CG())

  override fun <A, B> DayOf<F, G, A>.map(f: (A) -> B): Day<F, G, B> =
    fix().mapLazy(f)
}

fun <A> summon(ev: @given A = arrow.given): A = ev

@Proof(TypeProof.Extension)
fun <F, G> Day.Companion.comonad(CF: @given Comonad<F> = arrow.given, CG: @given Comonad<G> = arrow.given) : Comonad<DayPartialOf<F, G>> =
  object : DayComonad<F, G>{
    override fun CF(): Comonad<F> = CF

    override fun CG(): Comonad<G> = CG
  }


@undocumented
interface DayFunctor<F, G> : Functor<DayPartialOf<F, G>> {

  override fun <A, B> DayOf<F, G, A>.map(f: (A) -> B): Day<F, G, B> =
    fix().mapLazy(f)
}


@undocumented
interface DayApply<F, G> : Apply<DayPartialOf<F, G>> {
  fun AF(): Applicative<F>

  fun AG(): Applicative<G>

  override fun <A, B> DayOf<F, G, A>.map(f: (A) -> B): Day<F, G, B> =
    fix().mapLazy(f)

  override fun <A, B> Kind<DayPartialOf<F, G>, A>.ap(ff: Kind<DayPartialOf<F, G>, (A) -> B>): Day<F, G, B> =
    fix().ap(AF(), AG(), ff)
}


@undocumented
interface DayApplicative<F, G> : Applicative<DayPartialOf<F, G>> {
  fun AF(): Applicative<F>

  fun AG(): Applicative<G>

  override fun <A, B> DayOf<F, G, A>.map(f: (A) -> B): Day<F, G, B> =
    fix().mapLazy(f)

  override fun <A> just(a: A): Day<F, G, A> =
    Day.just(AF(), AG(), a)

  override fun <A, B> Kind<DayPartialOf<F, G>, A>.ap(ff: Kind<DayPartialOf<F, G>, (A) -> B>): Day<F, G, B> =
    fix().ap(AF(), AG(), ff)
}

@Proof(TypeProof.Extension)
fun <F, G> Day.Companion.applicative(AF: @given Applicative<F> = arrow.given, AG: @given Applicative<G> = arrow.given) : Applicative<DayPartialOf<F, G>> =
  object : DayApplicative<F, G>{
    override fun AF(): Applicative<F> = AF

    override fun AG(): Applicative<G> = AG
  }
