package arrow.ui

import arrow.Kind
import arrow.core.Eval
import arrow.core.test.UnitSpec
import arrow.core.test.generators.GenK
import arrow.core.test.laws.ComonadLaws
import arrow.typeclasses.Eq
import arrow.typeclasses.EqK
import arrow.ui.extensions.moore.comonad.comonad
import io.kotlintest.properties.Gen
import io.kotlintest.shouldBe

class MooreTest : UnitSpec() {

  init {

    fun <T> handle(x: T): Moore<T, T> = Moore(x, ::handle)
    fun <T> moore(t: T) = Moore(t, ::handle)

    fun <F> genk() = object : GenK<MoorePartialOf<F>> {
      override fun <A> genK(gen: Gen<A>): Gen<Kind<MoorePartialOf<F>, A>> =
        gen.map {
          moore(it)
        } as Gen<Kind<MoorePartialOf<F>, A>>
    }

    val EQK = object : EqK<MoorePartialOf<Int>> {
      override fun <A> Kind<MoorePartialOf<Int>, A>.eqK(other: Kind<MoorePartialOf<Int>, A>, EQ: Eq<A>): Boolean {
        return this.fix().extract() == other.fix().extract()
      }
    }

    testLaws(
      ComonadLaws.laws(Moore.comonad(), genk(), EQK)
    )

    fun handleRoute(route: String): Moore<String, Eval<String>> = when (route) {
      "About" -> Moore(Eval.just("About"), ::handleRoute)
      "Home" -> Moore(Eval.just("Home"), ::handleRoute)
      else -> Moore(Eval.just("???"), ::handleRoute)
    }

    val routerMoore = Moore(Eval.just("???"), ::handleRoute)

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
