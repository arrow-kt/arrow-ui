package arrow.ui

import arrow.Kind
import arrow.core.ForEval
import arrow.core.Eval
import arrow.core.Tuple2
import arrow.core.Tuple2Of
import arrow.core.extensions.eval.applicative.applicative
import arrow.core.extensions.eval.comonad.comonad
import arrow.core.fix
import arrow.core.test.UnitSpec
import arrow.core.test.generators.GenK
import arrow.core.test.laws.ApplicativeLaws
import arrow.core.test.laws.ComonadLaws
import arrow.core.value
import arrow.typeclasses.Eq
import arrow.typeclasses.EqK
import arrow.typeclasses.Hash
import arrow.ui.extensions.day.applicative.applicative
import arrow.ui.extensions.day.comonad.comonad
import arrow.ui.extensions.day.functor.functor
import io.kotlintest.properties.Gen
import io.kotlintest.shouldBe

class DayTest : UnitSpec() {
  init {

    val EQK = object : EqK<DayPartialOf<ForEval, ForEval>> {
      override fun <A> Kind<DayPartialOf<ForEval, ForEval>, A>.eqK(other: Kind<DayPartialOf<ForEval, ForEval>, A>, EQ: Eq<A>): Boolean =
        (this.fix() to other.fix()).let {
          EQ.run {
            it.first.extract(Eval.comonad(), Eval.comonad()).eqv(it.second.extract(Eval.comonad(), Eval.comonad()))
          }
        }
    }

    fun <F, G, X, Y> GENK(genkF: GenK<F>, genkG: GenK<G>, genX: Gen<X>, genY: Gen<Y>) = object : GenK<DayPartialOf<F, G>> {
      override fun <A> genK(gen: Gen<A>): Gen<Kind<DayPartialOf<F, G>, A>> {

        val genF = gen.map {
          { _: X, _: Y -> it }
        }

        return Gen.bind(genkF.genK(genX), genkG.genK(genY), genF) { a, b, f ->
          Day(a, b, f)
        }
      }
    }

    val gk = GENK(Eval.genK(), Eval.genK(), Gen.int(), Gen.int())

    testLaws(
      ApplicativeLaws.laws(Day.applicative(Eval.applicative(), Eval.applicative()), Day.functor(), gk, EQK),
      ComonadLaws.laws(Day.comonad(Eval.comonad(), Eval.comonad()), gk, EQK)
    )

    val get: (Int, Int) -> Tuple2<Int, Int> = { left, right -> Tuple2(left, right) }
    val day = Day(Eval.just(1), Eval.just(1), get)
    val compareSides = { left: Int, right: Int ->
      when {
        left > right -> "Left is greater"
        right > left -> "Right is greater"
        else -> "Both sides are equal"
      }
    }

    "Day extract should return the result of calling get with both sides" {
      day.extract(Eval.comonad(), Eval.comonad()) shouldBe Tuple2(1, 1)
    }

    @Suppress("ExplicitItLambdaParameter") // Required at runtime or else test fails
    "Day coflatmap should transform result type" {
      val d = day.coflatMap(Eval.comonad(), Eval.comonad()) { it: DayOf<ForEval, ForEval, Tuple2Of<Int, Int>> ->
        val (left, right) = it.fix().extract(Eval.comonad(), Eval.comonad()).fix()
        compareSides(left, right)
      }

      d.extract(Eval.comonad(), Eval.comonad()) shouldBe "Both sides are equal"
    }

    "Day map should transform result type" {
      val d = day.map {
        val (left, right) = it
        compareSides(left, right)
      }

      d.extract(Eval.comonad(), Eval.comonad()) shouldBe "Both sides are equal"
    }

    @Suppress("ExplicitItLambdaParameter") // Required at runtime or else test fails
    "Day coflatMapLazy should transform result type" {
      val d = day.coflatMapLazy(Eval.comonad(), Eval.comonad()) { it: DayOf<ForEval, ForEval, Tuple2Of<Int, Int>> ->
        val (left, right) = it.fix().extract(Eval.comonad(), Eval.comonad()).fix()
        compareSides(left, right)
      }

      d.extract(Eval.comonad(), Eval.comonad()) shouldBe "Both sides are equal"
    }

    "Day mapLazy should transform result type" {
      val d = day.mapLazy {
        val (left, right) = it
        compareSides(left, right)
      }

      d.extract(Eval.comonad(), Eval.comonad()) shouldBe "Both sides are equal"
    }
  }
}

internal fun Eval.Companion.genK(): GenK<ForEval> =
  object : GenK<ForEval> {
    override fun <A> genK(gen: Gen<A>): Gen<Kind<ForEval, A>> =
      gen.map { just(it) }
  }

internal fun Eval.Companion.eqK(): EqK<ForEval> =
  object : EqK<ForEval> {
    override fun <A> Kind<ForEval, A>.eqK(other: Kind<ForEval, A>, EQ: Eq<A>): Boolean =
      EQ.run { value().eqv(other.value()) }
  }

internal fun <A> Eval.Companion.eq(eq: Eq<A>): Eq<Eval<A>> =
  object : Eq<Eval<A>> {
    override fun Eval<A>.eqv(b: Eval<A>): Boolean =
      eq.run { value().eqv(b.value()) }
  }

internal fun <A> Eval.Companion.hash(h: Hash<A>): Hash<Eval<A>> =
  object : Hash<Eval<A>> {
    override fun Eval<A>.hashWithSalt(salt: Int): Int =
      h.run { value().hashWithSalt(salt) }
  }
