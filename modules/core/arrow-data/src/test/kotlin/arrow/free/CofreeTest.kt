package arrow.free

import arrow.Kind
import arrow.core.*
import arrow.data.*
import arrow.free.Cofree.Companion.unfold
import arrow.test.UnitSpec
import arrow.test.concurrency.SideEffect
import arrow.test.laws.ComonadLaws
import arrow.typeclasses.Eq
import arrow.core.FunctionK
import arrow.free.instances.ForCofree
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.matchers.shouldBe
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class CofreeTest : UnitSpec() {

  init {

    ForCofree<ForOption>() extensions {
      testLaws(ComonadLaws.laws(this, {
        val sideEffect = SideEffect()
        unfold(Option.functor(), sideEffect.counter, {
          sideEffect.increment()
          if (it % 2 == 0) None else Some(it + 1)
        })
      }, Eq { a, b ->
        a.fix().run().fix() == b.fix().run().fix()
      }))
    }

    "tailForced should evaluate and return" {
      val sideEffect = SideEffect()
      val start: Cofree<ForId, Int> = unfold(Id.functor(), sideEffect.counter, { sideEffect.increment(); Id(it + 1) })
      sideEffect.counter shouldBe 0
      start.tailForced()
      sideEffect.counter shouldBe 1
    }

    "runTail should run once and return" {
      val sideEffect = SideEffect()
      val start: Cofree<ForId, Int> = unfold(Id.functor(), sideEffect.counter, { sideEffect.increment(); Id(it) })
      sideEffect.counter shouldBe 0
      start.runTail()
      sideEffect.counter shouldBe 1
    }

    "run should fold until completion" {
      val sideEffect = SideEffect()
      val start: Cofree<ForOption, Int> = unfold(Option.functor(), sideEffect.counter, {
        sideEffect.increment()
        if (it == 5) None else Some(it + 1)
      })
      sideEffect.counter shouldBe 0
      start.run()
      sideEffect.counter shouldBe 6
      start.extract() shouldBe 0
    }

    "run with an stack-unsafe monad should blow up the stack" {
      try {
        val limit = 10000
        val counter = SideEffect()
        val startThousands: Cofree<ForOption, Int> = unfold(Option.functor(), counter.counter, {
          counter.increment()
          if (it == limit) None else Some(it + 1)
        })
        startThousands.run()
        throw AssertionError("Run should overflow on a stack-unsafe monad")
      } catch (e: StackOverflowError) {
        // Expected. For stack safety use cataM instead
      }
    }

    "run with an stack-safe monad should not blow up the stack" {
      val counter = SideEffect()
      val startThousands: Cofree<ForEval, Int> = unfold(Eval.functor(), counter.counter, {
        counter.increment()
        Eval.now(it + 1)
      })
      startThousands.run()
      counter.counter shouldBe 1
    }

    val startHundred: Cofree<ForOption, Int> = unfold(Option.functor(), 0, { if (it == 100) None else Some(it + 1) })

    "mapBranchingRoot should modify the value of the functor" {
      val mapped = startHundred.mapBranchingRoot(object : FunctionK<ForOption, ForOption> {
        override fun <A> invoke(fa: Kind<ForOption, A>): Kind<ForOption, A> =
          None
      })
      val expected = NonEmptyList.of(0)
      cofreeOptionToNel(mapped) shouldBe expected
    }

    "mapBranchingS/T should recur over S and T respectively" {
      val mappedS = startHundred.mapBranchingS(optionToList, ListK.functor())
      val mappedT = startHundred.mapBranchingT(optionToList, ListK.functor())
      val expected = NonEmptyList.fromListUnsafe((0..100).toList())
      cofreeListToNel(mappedS) shouldBe expected
      cofreeListToNel(mappedT) shouldBe expected
    }

    "cata should traverse the structure" {
      val cata: NonEmptyList<Int> = startHundred.cata<NonEmptyList<Int>>(
        { i, lb -> Eval.now(NonEmptyList(i, lb.fix().fold({ emptyList<Int>() }, { it.all }))) },
        Option.traverse()
      ).value()

      val expected = NonEmptyList.fromListUnsafe((0..100).toList())

      cata shouldBe expected
    }

    val startTwoThousand: Cofree<ForOption, Int> = unfold(Option.functor(), 0, { if (it == 2000) None else Some(it + 1) })

    with(startTwoThousand) {
      "cata with an stack-unsafe monad should blow up the stack" {
        try {
          cata<NonEmptyList<Int>>(
            { i, lb -> Eval.now(NonEmptyList(i, lb.fix().fold({ emptyList<Int>() }, { it.all }))) },
            Option.traverse()
          ).value()
          throw AssertionError("Run should overflow on a stack-unsafe monad")
        } catch (e: StackOverflowError) {
          // Expected. For stack safety use cataM instead
        }
      }

      "cataM should traverse the structure in a stack-safe way on a monad" {
        val folder: (Int, Kind<ForOption, NonEmptyList<Int>>) -> EvalOption<NonEmptyList<Int>> = { i, lb ->
          if (i <= 2000)
            OptionT.just(Eval.applicative(), NonEmptyList(i, lb.fix().fold({ emptyList<Int>() }, { it.all })))
          else
            OptionT.none(Eval.applicative())
        }
        val inclusion = object : FunctionK<ForEval, EvalOptionF> {
          override fun <A> invoke(fa: Kind<ForEval, A>): Kind<EvalOptionF, A> =
            OptionT(fa.fix().map { Some(it) })
        }
        val cataHundred = cataM(OptionT.monad(Eval.monad()), Option.traverse(), inclusion, folder).fix().value.fix().value()
        val newCof = Cofree(Option.functor(), 2001, Eval.now(Some(startTwoThousand)))
        val cataHundredOne = newCof.cataM(OptionT.monad(Eval.monad()), Option.traverse(), inclusion, folder).fix().value.fix().value()

        cataHundred shouldBe Some(NonEmptyList.fromListUnsafe((0..2000).toList()))
        cataHundredOne shouldBe None
      }
    }

  }
}

typealias EvalOption<A> = OptionTOf<ForEval, A>

typealias EvalOptionF = OptionTPartialOf<ForEval>
