package arrow.data

import arrow.core.Id
import arrow.core.Tuple2
import arrow.core.functor
import arrow.core.toT
import arrow.syntax.collections.stateTraverse
import arrow.test.UnitSpec
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.matchers.shouldBe
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class StateTests : UnitSpec() {

    private val addOne = State<Int, Int> { n -> n * 2 toT n }

    val add1 = State { n: Int -> n + 1 toT n }

    init {
        "addOne.run(1) should return Pair(2, 1)" {
            addOne.run(1) shouldBe Tuple2(2, 1)
        }

        "addOne.map(n -> n).run(1) should return same Pair(2, 1)" {
            addOne.map({ n -> n }, Id.functor()).run(1) shouldBe Tuple2(2, 1)
        }

        "addOne.map(n -> n.toString).run(1) should return same Pair(2, \"1\")" {
            addOne.map(Int::toString, Id.functor()).run(1) shouldBe Tuple2(2, "1")
        }

        "addOne.runS(1) should return 2" {
            addOne.runS(1) shouldBe 2
        }

        "addOne.runA(1) should return 1" {
            addOne.runA(1) shouldBe 1
        }

        "basic" {
            add1.run(1) shouldBe (2 toT 1)
        }

        "traverse" {
            val ns = (0..10).toList()
            val x = ns.stateTraverse { add1 }
            x.run(0).a shouldBe 11
        }

        "pure" {
            val s1 = StateApi.pure<String, Int>(1)
            s1.run("foo") shouldBe ("foo" toT 1)
        }

        "get" {
            val s1 = StateApi.get<String>()
            s1.run("foo") shouldBe ("foo" toT "foo")
        }

        "modify" {
            val s1 = StateApi.modify<String> { "bar" }
            val s2 = StateApi.set("bar")
            s1.run("foo") shouldBe s2.run("foo")
        }
    }
}
