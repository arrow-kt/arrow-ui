package arrow.ui

import arrow.Kind
import arrow.core.Const
import arrow.core.ForId
import arrow.core.Id
import arrow.core.extensions.const.eqK.eqK
import arrow.core.extensions.eq
import arrow.core.extensions.hash
import arrow.core.extensions.id.comonad.comonad
import arrow.core.extensions.id.eq.eq
import arrow.core.extensions.id.eqK.eqK
import arrow.core.extensions.id.functor.functor
import arrow.core.extensions.id.hash.hash
import arrow.core.fix
import arrow.test.UnitSpec
import arrow.test.generators.GenK
import arrow.test.generators.genK
import arrow.test.laws.ComonadLaws
import arrow.test.laws.HashLaws
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

    val genkSumId = genk(Id.genK(), Id.genK())

    val sumIdEQK = Sum.eqK(Id.eqK(), Id.eqK())
    val IDEQ = Eq<Kind<ForId, Int>> { a, b -> Id.eq(Int.eq()).run { a.fix().eqv(b.fix()) } }
    val IDH = Hash<Kind<ForId, Int>> { Id.hash(Int.hash()).run { it.fix().hash() } }

    val genSumConst =
      genk(Const.genK(Gen.int()), Const.genK(Gen.int()))

    val constEQK = Const.eqK(Int.eq())
    val sumConstEQK = Sum.eqK(constEQK, constEQK)

    testLaws(
      // TODO: tests fail when Sum.right is also generated. (https://github.com/arrow-kt/arrow/issues/1856)

      /*
       DivisibleLaws.laws(
        Sum.divisible(Const.divisible(Int.monoid()), Const.divisible(Int.monoid())),
        genSumConst,
        sumConstEQK
      ), */
      ComonadLaws.laws(Sum.comonad(Id.comonad(), Id.comonad()), genkSumId, sumIdEQK),
      HashLaws.laws(Sum.hash(IDH, IDH), genSum(), Sum.eq(IDEQ, IDEQ))
    )

    val abSum = Sum.left(Id.just("A"), Id.just("B"))

    "Sum extract should return the view of the current side" {
      abSum.extract(Id.comonad(), Id.comonad()) shouldBe "A"
    }

    "Sum changeSide should return the same Sum with desired side" {
      val sum = abSum.changeSide(Sum.Side.Right)

      sum.extract(Id.comonad(), Id.comonad()) shouldBe "B"
    }

    "Sum extend should transform view type" {
      val asciiValueFromLetter = { x: String -> x.first().toInt() }
      val sum = abSum.coflatmap(Id.comonad(), Id.comonad()) {
        when (it.side) {
          is Sum.Side.Left -> asciiValueFromLetter(it.left.fix().extract())
          is Sum.Side.Right -> asciiValueFromLetter(it.right.fix().extract())
        }
      }

      sum.extract(Id.comonad(), Id.comonad()) shouldBe 65
    }

    "Sum map should transform view type" {
      val asciiValueFromLetter = { x: String -> x.first().toInt() }
      val sum = abSum.map(Id.functor(), Id.functor(), asciiValueFromLetter)

      sum.extract(Id.comonad(), Id.comonad()) shouldBe 65
    }
  }
}

private fun genSum(): Gen<Sum<ForId, ForId, Int>> =
  Gen.int().map {
    Sum.left(Id.just(it), Id.just(it))
  }
