package arrow.free

import arrow.core.*
import arrow.free.instances.ForCoyoneda
import arrow.test.UnitSpec
import arrow.test.laws.FunctorLaws
import arrow.typeclasses.Eq
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.matchers.shouldBe
import io.kotlintest.properties.forAll
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class CoyonedaTest : UnitSpec() {
  val EQ: Eq<CoyonedaOf<ForId, Int, Int>> = Eq { a, b ->
    a.fix().lower(Id.functor()) == b.fix().lower(Id.functor())
  }

  init {

    ForCoyoneda<ForId, Int>() extensions {
      testLaws(FunctorLaws.laws(this, { Coyoneda(Id(0), { it }) }, EQ))
    }

    "map should be stack-safe" {
      val loops = 10000

      tailrec fun loop(n: Int, acc: Coyoneda<ForOption, Int, Int>): Coyoneda<ForOption, Int, Int> =
        if (n <= 0) acc
        else loop(n - 1, acc.map { it + 1 })

      val result = loop(loops, Coyoneda(Some(0), ::identity)).lower(Option.functor())
      val expected = Some(loops)

      expected shouldBe result
    }

    "toYoneda should convert to an equivalent Yoneda" {
      forAll { x: Int ->
        val op = Coyoneda(Id(x), Int::toString)
        val toYoneda = op.toYoneda(Id.functor()).lower().fix()
        val expected = Yoneda(Id(x.toString()), Id.functor()).lower().fix()

        expected == toYoneda
      }
    }
  }
}
