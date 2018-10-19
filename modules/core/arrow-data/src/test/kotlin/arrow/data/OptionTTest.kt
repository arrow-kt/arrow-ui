package arrow.data

import arrow.Kind
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.instances.nonemptylist.monad.monad
import arrow.instances.option.monad.monad
import arrow.instances.option.semigroup.semigroup
import arrow.instances.optiont.applicative.applicative
import arrow.instances.optiont.monad.monad
import arrow.instances.optiont.monoidK.monoidK
import arrow.instances.optiont.semigroupK.semigroupK
import arrow.mtl.instances.option.traverseFilter.traverseFilter
import arrow.mtl.instances.optiont.functorFilter.functorFilter
import arrow.mtl.instances.optiont.traverseFilter.traverseFilter
import arrow.test.UnitSpec
import arrow.test.laws.*
import arrow.typeclasses.Eq
import arrow.typeclasses.Monad
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.properties.forAll
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class OptionTTest : UnitSpec() {

  fun <A> EQ(): Eq<Kind<OptionTPartialOf<A>, Int>> = Eq { a, b ->
    a.value() == b.value()
  }

  fun <A> EQ_NESTED(): Eq<Kind<OptionTPartialOf<A>, Kind<OptionTPartialOf<A>, Int>>> = Eq { a, b ->
    a.value() == b.value()
  }

  val NELM: Monad<ForNonEmptyList> = NonEmptyList.monad()

  init {

    testLaws(
      MonadLaws.laws(OptionT.monad(Option.monad()), Eq.any()),
      SemigroupKLaws.laws(
        OptionT.semigroupK(Option.monad()),
        OptionT.applicative(Option.monad()),
        EQ()),

      MonoidKLaws.laws(
        OptionT.monoidK(Option.monad()),
        OptionT.applicative(Option.monad()),
        EQ()),

      FunctorFilterLaws.laws(
        OptionT.functorFilter(Option.monad()),
        { OptionT(Some(Some(it))) },
        EQ()),

      TraverseFilterLaws.laws(
        OptionT.traverseFilter(Option.traverseFilter()),
        OptionT.applicative(Option.monad()),
        { OptionT(Some(Some(it))) },
        EQ(),
        EQ_NESTED())
    )

    "toLeft for Some should build a correct EitherT" {
      forAll { a: Int, b: String ->
        OptionT.fromOption(this.NELM, Some(a)).toLeft(this.NELM) { b } == EitherT.left<ForNonEmptyList, Int, String>(this.NELM, a)
      }
    }

    "toLeft for None should build a correct EitherT" {
      forAll { a: Int, b: String ->
        OptionT.fromOption<ForNonEmptyList, Int>(this.NELM, None).toLeft(this.NELM) { b } == EitherT.right<ForNonEmptyList, Int, String>(this.NELM, b)
      }
    }

    "toRight for Some should build a correct EitherT" {
      forAll { a: Int, b: String ->
        OptionT.fromOption(this.NELM, Some(b)).toRight(this.NELM) { a } == EitherT.right<ForNonEmptyList, Int, String>(this.NELM, b)
      }
    }

    "toRight for None should build a correct EitherT" {
      forAll { a: Int, b: String ->
        OptionT.fromOption<ForNonEmptyList, String>(this.NELM, None).toRight(this.NELM) { a } == EitherT.left<ForNonEmptyList, Int, String>(this.NELM, a)
      }
    }

  }
}
