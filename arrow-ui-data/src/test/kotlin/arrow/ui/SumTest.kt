package arrow.ui

import arrow.Kind
import arrow.core.ForEval
import arrow.core.Eval
import arrow.core.extensions.eq
import arrow.core.extensions.eval.comonad.comonad
import arrow.core.extensions.eval.functor.functor
import arrow.core.extensions.hash
import arrow.core.fix
import arrow.core.test.UnitSpec
import arrow.core.test.generators.GenK
import arrow.core.test.laws.ComonadLaws
import arrow.core.test.laws.HashLaws
import arrow.typeclasses.Eq
import arrow.typeclasses.Hash
import arrow.ui.extensions.sum.comonad.comonad
import arrow.ui.extensions.sum.eq.eq
import arrow.ui.extensions.sum.eqK.eqK
import arrow.ui.extensions.sum.hash.hash
import io.kotlintest.properties.Gen
import io.kotlintest.shouldBe

class SumTest : UnitSpec() {
  fun <F, G> genk(GENKF: GenK<F>, GENKG: GenK<G>) = object : GenK<SumPartialOf<F, G>> {
    override fun <V> genK(gen: Gen<V>): Gen<Kind<SumPartialOf<F, G>, V>> = Gen.oneOf(
      Gen.bind(GENKF.genK(gen), GENKG.genK(gen)) { a, b ->
        Sum.left(a, b)
      },
      Gen.bind(GENKF.genK(gen), GENKG.genK(gen)) { a, b ->
        Sum.right(a, b)
      }
    )
  }

  init {

    val genkSumEval = genk(Eval.genK(), Eval.genK())

    val sumEvalEQK = Sum.eqK(Eval.eqK(), Eval.eqK())
    val IDEQ = Eq<Kind<ForEval, Int>> { a, b -> Eval.eq(Int.eq()).run { a.fix().eqv(b.fix()) } }
    val IDH = Eval.hash(Int.hash()) as Hash<Kind<ForEval, Int>>

    // val genSumConst = genk(Const.genK(Gen.int()), Const.genK(Gen.int()))
    // val constEQK = Const.eqK(Int.eq())
    // val sumConstEQK = Sum.eqK(constEQK, constEQK)

    testLaws(
      // TODO: tests fail when Sum.right is also generated. (https://github.com/arrow-kt/arrow/issues/1856)

      /*
       DivisibleLaws.laws(
        Sum.divisible(Const.divisible(Int.monoid()), Const.divisible(Int.monoid())),
        genSumConst,
        sumConstEQK
      ), */
      ComonadLaws.laws(Sum.comonad(Eval.comonad(), Eval.comonad()), genkSumEval, sumEvalEQK),
      HashLaws.laws(Sum.hash(IDH, IDH), genSum(), Sum.eq(IDEQ, IDEQ))
    )

    val abSum = Sum.left(Eval.just("A"), Eval.just("B"))

    "Sum extract should return the view of the current side" {
      abSum.extract(Eval.comonad(), Eval.comonad()) shouldBe "A"
    }

    "Sum changeSide should return the same Sum with desired side" {
      val sum = abSum.changeSide(Sum.Side.Right)

      sum.extract(Eval.comonad(), Eval.comonad()) shouldBe "B"
    }

    "Sum extend should transform view type" {
      val asciiValueFromLetter = { x: String -> x.first().toInt() }
      val sum = abSum.coflatmap(Eval.comonad(), Eval.comonad()) {
        when (it.side) {
          is Sum.Side.Left -> asciiValueFromLetter(it.left.fix().extract())
          is Sum.Side.Right -> asciiValueFromLetter(it.right.fix().extract())
        }
      }

      sum.extract(Eval.comonad(), Eval.comonad()) shouldBe 65
    }

    "Sum map should transform view type" {
      val asciiValueFromLetter = { x: String -> x.first().toInt() }
      val sum = abSum.map(Eval.functor(), Eval.functor(), asciiValueFromLetter)

      sum.extract(Eval.comonad(), Eval.comonad()) shouldBe 65
    }
  }
}

private fun genSum(): Gen<Sum<ForEval, ForEval, Int>> =
  Gen.int().map {
    Sum.left(Eval.just(it), Eval.just(it))
  }
