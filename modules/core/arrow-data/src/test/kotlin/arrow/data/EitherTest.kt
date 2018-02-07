package arrow.data

import arrow.Kind
import arrow.core.*
import arrow.test.UnitSpec
import arrow.test.laws.EqLaws
import arrow.test.laws.MonadErrorLaws
import arrow.test.laws.SemigroupKLaws
import arrow.test.laws.TraverseLaws
import arrow.typeclasses.*
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.matchers.shouldNotBe
import io.kotlintest.properties.forAll
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class EitherTest : UnitSpec() {
    val EQ: Eq<Kind<EitherPartialOf<ForId>, Int>> = Eq { a, b ->
        a.reify() == b.reify()
    }

    init {

        "instances can be resolved implicitly" {
            functor<EitherPartialOf<Throwable>>() shouldNotBe null
            applicative<EitherPartialOf<Throwable>>() shouldNotBe null
            monad<EitherPartialOf<Throwable>>() shouldNotBe null
            foldable<EitherPartialOf<Throwable>>() shouldNotBe null
            traverse<EitherPartialOf<Throwable>>() shouldNotBe null
            applicativeError<EitherPartialOf<Throwable>, Throwable>() shouldNotBe null
            monadError<EitherPartialOf<Throwable>, Throwable>() shouldNotBe null
            semigroupK<EitherPartialOf<Throwable>>() shouldNotBe null
            eq<Either<String, Int>>() shouldNotBe null
        }

        testLaws(
            EqLaws.laws(eq<Either<String, Int>>(), { Right(it) }),
            MonadErrorLaws.laws(Either.monadError(), Eq.any(), Eq.any()),
            TraverseLaws.laws(Either.traverse<Throwable>(), Either.applicative(), { Right(it) }, Eq.any()),
            SemigroupKLaws.laws(Either.semigroupK(), Either.applicative(), EQ)
        )

        "getOrElse should return value" {
            forAll { a: Int, b: Int ->
                Right(a).getOrElse { b } == a
                        && Left(a).getOrElse { b } == b
            }

        }

        "getOrHandle should return value" {
            forAll { a: Int, b: Int ->
                Right(a).getOrHandle { b } == a
                        && Left(a).getOrHandle { it + b } == a + b
            }

        }

        "filterOrElse should filters value" {
            forAll { a: Int, b: Int ->
                    val left: Either<Int, Int> = Left(a)

                    Right(a).filterOrElse({ it > a - 1 }, { b }) == Right(a)
                            && Right(a).filterOrElse({ it > a + 1 }, { b }) == Left(b)
                            && left.filterOrElse({ it > a - 1 }, { b }) == Left(a)
                            && left.filterOrElse({ it > a + 1 }, { b }) == Left(a)
            }
        }

        "swap should interchange values" {
            forAll { a: Int ->
                Left(a).swap() == Right(a)
                        && Right(a).swap() == Left(a)
            }
        }

        "toOption should convert" {
            forAll { a: Int ->
                Right(a).toOption() == Some(a)
                        && Left(a).toOption() == None
            }
        }

        "contains should check value" {
            forAll { a: Int, b: Int ->
                Right(a).contains(a)
                        && !Right(a).contains(b)
                        && !Left(a).contains(a)
            }
        }

        "mapLeft should alter left instance only" {
            forAll{ a: Int, b: Int ->
                val right: Either<Int, Int> = Right(a)
                val left: Either<Int, Int> = Left(b)
                right.mapLeft { it + 1 } == right && left.mapLeft { it+1 } == Left(b + 1)
            }
        }

        "cond should create right instance only if test is true" {
            forAll{t: Boolean, i: Int, s: String ->
                val expected = if (t) Right(i) else Left(s)
                Either.cond(t, { i }, { s }) == expected
            }
        }

    }
}
