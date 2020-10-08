package arrow.ui

import arrow.Kind
import arrow.core.ForId
import arrow.core.Id
import arrow.core.extensions.id.comonad.comonad
import arrow.core.fix
import arrow.core.test.UnitSpec
import arrow.core.test.generators.GenK
import arrow.core.test.generators.genK
import arrow.core.test.laws.MonadLaws
import arrow.typeclasses.Eq
import arrow.typeclasses.EqK
import arrow.ui.extensions.cot.monad.monad
import arrow.ui.extensions.functor
import io.kotlintest.properties.Gen

class CoTest : UnitSpec() {
  init {
    val EQK = object : EqK<CoPartialOf<ForId>> {
      override fun <A> Kind<CoPartialOf<ForId>, A>.eqK(other: Kind<CoPartialOf<ForId>, A>, EQ: Eq<A>): Boolean {
        (this.fix() to other.fix()).let {
          EQ.run {
            Id.comonad().run {
              return pair(this).zap(this, Co().functor(), Id.just { a -> a }, this@eqK) ==
                pair(this).zap(this, Co().functor(), Id.just { a -> a }, other)
            }
          }
        }
      }
    }

    fun GENK(genkF: GenK<ForId>) = object : GenK<CoPartialOf<ForId>> {
      override fun <A> genK(gen: Gen<A>): Gen<Kind<CoPartialOf<ForId>, A>> = genkF.genK(gen).map { k ->
        Co(Id.comonad()) { _ -> k.fix().map { it as Any } }
      }
    }

    testLaws(
      MonadLaws.laws(Co.monad(Id.comonad()), GENK(Id.genK()), EQK)
    )
  }
}
