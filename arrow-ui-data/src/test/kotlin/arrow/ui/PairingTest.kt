package arrow.ui

import arrow.core.ForId
import arrow.core.Id
import arrow.core.extensions.id.applicative.applicative
import arrow.core.extensions.id.monad.monad
import arrow.core.identity
import arrow.core.test.UnitSpec
import arrow.mtl.State
import arrow.mtl.extensions.functor
import arrow.mtl.extensions.fx
import arrow.mtl.fix
import arrow.ui.extensions.functor
import io.kotlintest.shouldBe

class PairingTest : UnitSpec() {
  init {

    "Test Pairing State <-> Store" {
      val w = Store(0, ::identity)
      val actions = with (State()) { fx<Int, Unit> {
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

  }
}
