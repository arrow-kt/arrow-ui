package arrow.data

import arrow.core.*
import arrow.test.UnitSpec
import arrow.test.laws.MonadErrorLaws
import arrow.test.laws.SemigroupKLaws
import arrow.test.laws.TraverseLaws
import arrow.typeclasses.*
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.matchers.shouldNotBe
import io.kotlintest.properties.forAll
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class EitherTTest : UnitSpec() {
    init {

        "instances can be resolved implicitly" {
            functor<EitherTPartialOf<ForOption, Throwable>>() shouldNotBe null
            applicative<EitherTPartialOf<ForOption, Throwable>>() shouldNotBe null
            monad<EitherTPartialOf<ForOption, Throwable>>() shouldNotBe null
            foldable<EitherTPartialOf<ForOption, Throwable>>() shouldNotBe null
            traverse<EitherTPartialOf<ForOption, Throwable>>() shouldNotBe null
            applicativeError<EitherTPartialOf<ForOption, Throwable>, Throwable>() shouldNotBe null
            monadError<EitherTPartialOf<ForOption, Throwable>, Throwable>() shouldNotBe null
            semigroupK<EitherTPartialOf<ForOption, Throwable>>() shouldNotBe null
        }

        testLaws(
            MonadErrorLaws.laws(EitherT.monadError<ForId, Throwable>(Id.monad()), Eq.any(), Eq.any()),
            TraverseLaws.laws(EitherT.traverse<ForId, Int>(), EitherT.applicative(), { EitherT(Id(Right(it))) }, Eq.any()),
            SemigroupKLaws.laws<EitherTPartialOf<ForId, Int>>(
                EitherT.semigroupK(Id.monad()),
                EitherT.applicative(Id.monad()),
                Eq.any())
        )

        "mapLeft should alter left instance only" {
            forAll { i: Int, j: Int ->
                val left: Either<Int, Int> = Left(i)
                val right: Either<Int, Int> = Right(j)
                EitherT(Option(left)).mapLeft({it + 1}, Option.functor()) == EitherT(Option(Left(i+1))) &&
                        EitherT(Option(right)).mapLeft({it + 1}, Option.functor()) ==  EitherT(Option(right)) &&
                        EitherT(Option.empty<Either<Int, Int>>()).mapLeft({it +1}, Option.functor()) == EitherT(Option.empty<Either<Int, Int>>())
            }
        }

    }
}
