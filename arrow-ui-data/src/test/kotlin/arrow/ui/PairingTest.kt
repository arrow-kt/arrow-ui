package arrow.ui

import arrow.core.identity
import arrow.core.test.UnitSpec
import arrow.mtl.State
import arrow.mtl.extensions.functor
import arrow.mtl.extensions.fx
import arrow.mtl.fix
import arrow.ui.extensions.functor
import arrow.ui.extensions.fx
import arrow.ui.extensions.moore.comonad.comonad
import arrow.ui.extensions.moore.comonad.duplicate
import arrow.ui.extensions.moore.functor.functor
import arrow.ui.extensions.pairInputMoore
import arrow.ui.extensions.pairStateStore
import io.kotlintest.shouldBe

class PairingTest : UnitSpec() {
  init {

    "Test Pairing State <-> Store" {
      val w = Store(0, ::identity)
      val actions = with(State()) {
        fx<Int, Unit> {
          !set(5)
          !modify { x: Int -> x + 5 }
          val s = get<Int>()
          !set(!s * 3 + 1)
        }.fix()
      }
      val w2 = Pairing.pairStateStore<Int>().select(
        State().functor(),
        Store().functor(),
        actions,
        w.duplicate()
      ).fix()

      w2.extract() shouldBe 31
    }

    "Test Pairing MooreInput <-> Moore" {
      fun render(n: Int): String = if (n % 2 == 0) "$n is even" else "$n is odd"

      fun update(state: Int, action: Input): Int = when (action) {
        Input.Increment -> state + 1
        Input.Decrement -> state - 1
      }

      val w = Moore.from(0, ::render, ::update)

      val actions = with(MooreInput()) {
        fx<Input, Unit> {
          !from(Moore.comonad(), Input.Increment)
          !from(Moore.comonad(), Input.Increment)
          !from(Moore.comonad(), Input.Decrement)
          !from(Moore.comonad(), Input.Increment)
          !from(Moore.comonad(), Input.Increment)
        }.fix()
      }

      val w2 = Pairing.pairInputMoore<Input>(Moore.comonad()).select(
        MooreInput().functor(),
        Moore.functor(),
        actions,
        w.duplicate()
      ).fix()

      w2.view shouldBe "3 is odd"
    }
  }

  private enum class Input {
    Increment, Decrement;
  }
}
