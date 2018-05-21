package arrow.data

import arrow.Kind
import arrow.core.*
import arrow.instances.ForWriterT
import arrow.instances.monoid
import arrow.test.UnitSpec
import arrow.test.generators.genIntSmall
import arrow.test.generators.genTuple
import arrow.test.laws.MonadFilterLaws
import arrow.test.laws.MonadLaws
import arrow.test.laws.MonadWriterLaws
import arrow.test.laws.MonoidKLaws
import arrow.typeclasses.Eq
import io.kotlintest.KTestJUnitRunner
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class WriterTTest : UnitSpec() {
  init {

    ForWriterT(NonEmptyList.monad(), Int.monoid()) extensions {
      testLaws(
        MonadLaws.laws(this, Eq.any()),
        MonoidKLaws.laws(
          WriterT.monoidK<ForListK, Int>(ListK.monoidK()),
          WriterT.applicative(ListK.monad(), Int.monoid()),
          Eq { a, b ->
            a.fix().value == b.fix().value
          }),

        MonadWriterLaws.laws(WriterT.monad(Option.monad(), Int.monoid()),
          WriterT.monadWriter(Option.monad(), Int.monoid()),
          Int.monoid(),
          genIntSmall(),
          genTuple(genIntSmall(), genIntSmall()),
          Eq { a, b ->
            a.fix().value.fix().let { optionA: Option<Tuple2<Int, Int>> ->
              val optionB = b.fix().value.fix()
              optionA.fold({ optionB.fold({ true }, { false }) }, { value: Tuple2<Int, Int> -> optionB.fold({ false }, { value == it }) })
            }
          },
          Eq { a, b ->
            a.fix().value.fix().let { optionA: Option<Tuple2<Int, Tuple2<Int, Int>>> ->
              val optionB = b.fix().value.fix()
              optionA.fold({ optionB.fold({ true }, { false }) }, { value: Tuple2<Int, Tuple2<Int, Int>> -> optionB.fold({ false }, { value == it }) })
            }
          }
        ),

        MonadFilterLaws.laws(WriterT.monadFilter(Option.monadFilter(), Int.monoid()),
          { WriterT(Option(Tuple2(it, it))) },
          object : Eq<Kind<WriterTPartialOf<ForOption, Int>, Int>> {
            override fun Kind<WriterTPartialOf<ForOption, Int>, Int>.eqv(b: Kind<WriterTPartialOf<ForOption, Int>, Int>): Boolean =
              fix().value.fix().let { optionA: Option<Tuple2<Int, Int>> ->
                val optionB = b.fix().value.fix()
                optionA.fold({ optionB.fold({ true }, { false }) }, { value: Tuple2<Int, Int> -> optionB.fold({ false }, { value == it }) })
              }
          })
      )
    }

  }
}
