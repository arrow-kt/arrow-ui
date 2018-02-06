package arrow.data

import arrow.instances.IntMonoid
import arrow.mtl.traverseFilter
import arrow.test.UnitSpec
import arrow.test.laws.ApplicativeLaws
import arrow.test.laws.EqLaws
import arrow.test.laws.TraverseFilterLaws
import arrow.typeclasses.*
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.matchers.shouldNotBe
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class ConstTest : UnitSpec() {
    init {

        "instances can be resolved implicitly" {
            functor<ConstKindPartial<Int>>() shouldNotBe null
            applicative<ConstKindPartial<Int>>() shouldNotBe null
            foldable<ConstKindPartial<Int>>() shouldNotBe null
            traverse<ConstKindPartial<Int>>() shouldNotBe null
            traverseFilter<ConstKindPartial<Int>>() shouldNotBe null
            semigroup<ConstKind<Int, Int>>() shouldNotBe null
            monoid<ConstKind<Int, Int>>() shouldNotBe null
            eq<Const<Int, String>>() shouldNotBe null
        }

        testLaws(
            TraverseFilterLaws.laws(Const.traverseFilter(), Const.applicative(IntMonoid), { Const(it) }, Eq.any()),
            ApplicativeLaws.laws(Const.applicative(IntMonoid), Eq.any()),
            EqLaws.laws(eq<Const<Int, String>>(), { Const(it) })
        )
    }
}
