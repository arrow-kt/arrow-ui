package arrow.data

import arrow.core.*
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.matchers.shouldNotBe
import org.junit.runner.RunWith
import arrow.test.UnitSpec
import arrow.test.laws.*
import arrow.typeclasses.*

@RunWith(KTestJUnitRunner::class)
class TupleTest : UnitSpec() {
    init {

        "instances can be resolved implicitly" {
            functor<Tuple2<Int, Int>>() shouldNotBe null
            applicative<Tuple2<Int, Int>>() shouldNotBe null
            monad<Tuple2<Int, Int>>() shouldNotBe null
            comonad<Tuple2<Int, Int>>() shouldNotBe null
            foldable<Tuple2<Int, Int>>() shouldNotBe null
            traverse<Tuple2<Int, Int>>() shouldNotBe null
            monoid<Tuple2<Int, Int>>() shouldNotBe null

            eq<Tuple2<Int, Int>>() shouldNotBe null
            show<Tuple2<Int, Int>>() shouldNotBe null
            eq<Tuple3<Int, Int, Int>>() shouldNotBe null
            show<Tuple3<Int, Int, Int>>() shouldNotBe null
            eq<Tuple4<Int, Int, Int, Int>>() shouldNotBe null
            show<Tuple4<Int, Int, Int, Int>>() shouldNotBe null
            eq<Tuple5<Int, Int, Int, Int, Int>>() shouldNotBe null
            show<Tuple5<Int, Int, Int, Int, Int>>() shouldNotBe null
            eq<Tuple6<Int, Int, Int, Int, Int, Int>>() shouldNotBe null
            show<Tuple6<Int, Int, Int, Int, Int, Int>>() shouldNotBe null
            eq<Tuple7<Int, Int, Int, Int, Int, Int, Int>>() shouldNotBe null
            show<Tuple7<Int, Int, Int, Int, Int, Int, Int>>() shouldNotBe null
            eq<Tuple8<Int, Int, Int, Int, Int, Int, Int, Int>>() shouldNotBe null
            show<Tuple8<Int, Int, Int, Int, Int, Int, Int, Int>>() shouldNotBe null
            eq<Tuple9<Int, Int, Int, Int, Int, Int, Int, Int, Int>>() shouldNotBe null
            show<Tuple9<Int, Int, Int, Int, Int, Int, Int, Int, Int>>() shouldNotBe null
            eq<Tuple10<Int, Int, Int, Int, Int, Int, Int, Int, Int, Int>>() shouldNotBe null
            show<Tuple10<Int, Int, Int, Int, Int, Int, Int, Int, Int, Int>>() shouldNotBe null
        }

        testLaws(
            MonadLaws.laws(Tuple2.monad<Int>(), Eq.any()),
            ComonadLaws.laws(Tuple2.comonad(), { 0 toT it }, Eq.any()),
            TraverseLaws.laws(Tuple2.traverse(), Tuple2.functor(), { 0 toT it }, Eq.any()),
            EqLaws.laws { Tuple2(it, it) },
            ShowLaws.laws { Tuple2(it, it) },
            EqLaws.laws { Tuple3(it, it, it) },
            ShowLaws.laws { Tuple3(it, it, it) },
            EqLaws.laws { Tuple4(it, it, it, it) },
            ShowLaws.laws { Tuple4(it, it, it, it) },
            EqLaws.laws { Tuple5(it, it, it, it, it) },
            ShowLaws.laws { Tuple5(it, it, it, it, it) },
            EqLaws.laws { Tuple6(it, it, it, it, it, it) },
            ShowLaws.laws { Tuple6(it, it, it, it, it, it) },
            EqLaws.laws { Tuple7(it, it, it, it, it, it, it) },
            ShowLaws.laws { Tuple7(it, it, it, it, it, it, it) },
            EqLaws.laws { Tuple8(it, it, it, it, it, it, it, it) },
            ShowLaws.laws { Tuple8(it, it, it, it, it, it, it, it) },
            EqLaws.laws { Tuple9(it, it, it, it, it, it, it, it, it) },
            ShowLaws.laws { Tuple9(it, it, it, it, it, it, it, it, it) },
            EqLaws.laws { Tuple10(it, it, it, it, it, it, it, it, it, it) },
            ShowLaws.laws { Tuple10(it, it, it, it, it, it, it, it, it, it) }
        )
    }
}
