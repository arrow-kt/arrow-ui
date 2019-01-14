package arrow.data

import arrow.Kind
import arrow.Kind3
import arrow.core.ForId
import arrow.core.Id
import arrow.core.Right
import arrow.core.extensions.eq
import arrow.core.extensions.hash
import arrow.core.fix
import arrow.data.extensions.coproduct.comonad.comonad
import arrow.data.extensions.coproduct.eq.eq
import arrow.data.extensions.coproduct.functor.functor
import arrow.data.extensions.coproduct.hash.hash
import arrow.data.extensions.coproduct.traverse.traverse
import arrow.core.extensions.id.comonad.comonad
import arrow.core.extensions.id.eq.eq
import arrow.core.extensions.id.functor.functor
import arrow.core.extensions.id.hash.hash
import arrow.core.extensions.id.traverse.traverse
import arrow.test.UnitSpec
import arrow.test.laws.ComonadLaws
import arrow.test.laws.HashLaws
import arrow.test.laws.TraverseLaws
import arrow.typeclasses.Eq
import arrow.typeclasses.Hash
import io.kotlintest.runner.junit4.KotlinTestRunner
import org.junit.runner.RunWith

@RunWith(KotlinTestRunner::class)
class CoproductTest : UnitSpec() {
  val EQ: Eq<Kind3<ForCoproduct, ForId, ForId, Int>> = Eq { a, b ->
    a.fix().fix() == b.fix().fix()
  }

  init {
    val TF = Coproduct.traverse(Id.traverse(), Id.traverse())
    val FF = Coproduct.functor(Id.functor(), Id.functor())
    val CM = Coproduct.comonad(Id.comonad(), Id.comonad())

    val IDEQ = Eq<Kind<ForId, Int>> { a, b -> Id.eq(Int.eq()).run { a.fix().eqv(b.fix()) } }
    val IDH = Hash<Kind<ForId, Int>> { Id.hash(Int.hash()).run { it.fix().hash() } }

    testLaws(
      TraverseLaws.laws(TF, FF, { Coproduct(Right(Id(it))) }, EQ),
      ComonadLaws.laws(CM, { Coproduct(Right(Id(it))) }, EQ),
      HashLaws.laws(Coproduct.hash(IDH, IDH), Coproduct.eq(IDEQ, IDEQ)) { Coproduct(Right(Id(it))) }
    )

  }
}
