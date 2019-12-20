package arrow.ui

import arrow.Kind
import arrow.core.Id
import arrow.test.UnitSpec
import arrow.test.laws.ComonadLaws
import arrow.typeclasses.Eq
import arrow.typeclasses.EqK
import arrow.ui.extensions.moore.comonad.comonad
import io.kotlintest.properties.Gen
import io.kotlintest.shouldBe

class MooreTest : UnitSpec() {

  init {

    fun handle(x: Int): Moore<Int, Int> = Moore(x, ::handle)
    val intMoore: (Int) -> MooreOf<Int, Int> = { x: Int -> Moore(x, ::handle) }
    val g = Gen.int().map(intMoore)

    val EQK = object : EqK<MoorePartialOf<Int>> {
      override fun <A> Kind<MoorePartialOf<Int>, A>.eqK(other: Kind<MoorePartialOf<Int>, A>, EQ: Eq<A>): Boolean {
        return this.fix().extract() == other.fix().extract()
      }
    }

    testLaws(
      ComonadLaws.laws(Moore.comonad(), g, EQK)
    )

    fun handleRoute(route: String): Moore<String, Id<String>> = when (route) {
      "About" -> Moore(Id("About"), ::handleRoute)
      "Home" -> Moore(Id("Home"), ::handleRoute)
      else -> Moore(Id("???"), ::handleRoute)
    }

    val routerMoore = Moore(Id("???"), ::handleRoute)

    "routerMoore view should be about after sending about event" {
      val currentRoute = routerMoore
        .handle("About")
        .extract()
        .extract()

      currentRoute shouldBe "About"
    }

    "routerMoore view should be home after sending home event" {
      val currentRoute = routerMoore
        .handle("About")
        .handle("Home")
        .extract()
        .extract()

      currentRoute shouldBe "Home"
    }

    "routerMoore view should be 0 after extending it for transforming view type" {
      val currentRoute = routerMoore
        .coflatMap { (view) ->
          when (view.extract()) {
            "About" -> 1
            "Home" -> 2
            else -> 0
          }
        }
        .extract()

      currentRoute shouldBe 0
    }

    "routerMoore view should be 0 after mapping it for transforming view type" {
      val currentRoute = routerMoore
        .map {
          when (it.extract()) {
            "About" -> 1
            "Home" -> 2
            else -> 0
          }
        }
        .extract()

      currentRoute shouldBe 0
    }
  }
}
