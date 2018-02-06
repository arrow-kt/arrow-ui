package arrow.data

import arrow.HK3
import arrow.core.*
import arrow.syntax.comonad.extract
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.matchers.shouldNotBe
import org.junit.runner.RunWith
import arrow.test.UnitSpec
import arrow.test.laws.ComonadLaws
import arrow.test.laws.TraverseLaws
import arrow.typeclasses.*

@RunWith(KTestJUnitRunner::class)
class CoproductTest : UnitSpec() {
    val EQ: Eq<HK3<CoproductHK, IdHK, IdHK, Int>> = Eq { a, b ->
        a.ev().extract() == b.ev().extract()
    }

    init {

        "instances can be resolved implicitly" {
            functor<CoproductKindPartial<IdHK, NonEmptyListHK>>() shouldNotBe null
            comonad<CoproductKindPartial<IdHK, NonEmptyListHK>>()  shouldNotBe null
            foldable<CoproductKindPartial<IdHK, NonEmptyListHK>>() shouldNotBe null
            traverse<CoproductKindPartial<IdHK, NonEmptyListHK>>() shouldNotBe null
        }

        testLaws(
            TraverseLaws.laws(traverse(), functor(), { Coproduct(Right(Id(it))) }, EQ),
            ComonadLaws.laws(comonad(), { Coproduct(Right(Id(it))) }, EQ)
        )

    }
}
