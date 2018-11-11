package arrow.data

import arrow.core.*
import arrow.instances.`try`.applicative.map
import arrow.instances.`try`.eq.eq
import arrow.instances.`try`.functor.functor
import arrow.instances.`try`.monadError.monadError
import arrow.instances.`try`.monoid.monoid
import arrow.instances.`try`.semigroup.semigroup
import arrow.instances.`try`.show.show
import arrow.instances.`try`.traverse.traverse
import arrow.instances.combine
import arrow.instances.monoid
import arrow.instances.semigroup
import arrow.mtl.instances.`try`.functorFilter.functorFilter
import arrow.test.UnitSpec
import arrow.test.laws.*
import arrow.typeclasses.Eq
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.matchers.*
import io.kotlintest.properties.forAll
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class TryTest : UnitSpec() {

  val success = Try { "10".toInt() }
  val failure = Try { "NaN".toInt() }

  init {

    val EQ = Try.eq(Eq<Any> { a, b -> a::class == b::class }, Eq.any())

    testLaws(
      SemigroupLaws.laws(Try.semigroup(Int.semigroup()), Try.just(1), Try.just(2), Try.just(3), EQ),
      MonoidLaws.laws(Try.monoid(MO = Int.monoid()), Try.just(1), EQ),
      EqLaws.laws(EQ) { Try.just(it) },
      ShowLaws.laws(Try.show(), EQ) { Try.just(it) },
      MonadErrorLaws.laws(Try.monadError(), Eq.any(), Eq.any()),
      TraverseLaws.laws(Try.traverse(), Try.functor(), ::Success, Eq.any()),
      FunctorFilterLaws.laws(Try.functorFilter(), { Try.just(it) }, Eq.any())
    )

    "empty should return a Success of the empty of the inner type" {
      Success(String.monoid().run { empty() }) shouldBe Try.monoid(String.monoid()).run { empty() }
    }

    "combine two Successes should return a Success of the combine of the inners" {
      forAll { a: String, b: String ->
        String.monoid().run { Try.just(a.combine(b)) } == Try.just(a).combine(String.monoid(), Try.just(b))
      }
    }

    "combine two Failures should return the second failure" {
      val throwable1 = Exception("foo")
      val throwable2 = Exception("foo")

      Try.raise<String>(throwable2) == Try.raise<String>(throwable1).combine(String.monoid(), Try.raise(throwable2))
    }

    "combine a Success and a Failure should return Failure" {
      val throwable = Exception("foo")
      val string = "String"

      Try.raise<String>(throwable) == Try.raise<String>(throwable).combine(String.monoid(), Try.just(string))
      Try.raise<String>(throwable) == Try.just(string).combine(String.monoid(), Try.raise(throwable))
    }

    "invoke of any should be success" {
      Try.invoke { 1 } shouldBe Success(1)
    }

    "invoke of exception should be failure" {
      val ex = Exception()
      Try.invoke { throw ex } shouldBe Failure(ex)
    }

    "filter evaluates predicate" {
      val failure: Try<Int> = Failure(Exception())

      Success(1).filter { true } shouldBe Success(1)
      Success(1).filter { false } shouldBe Failure(TryException.PredicateException("Predicate does not hold for 1"))
      failure.filter { true } shouldBe failure
      failure.filter { false } shouldBe failure
    }

    "failed tries to swap" {
      val ex = Exception()
      val failure: Try<Int> = Failure(ex)

      Success(1).failed() shouldBe Failure(TryException.UnsupportedOperationException("Success"))
      failure.failed() shouldBe Success(ex)
    }

    "fold should call left function on Failure" {
      Failure(Exception()).fold({ 2 }, { 3 }) shouldBe 2
    }

    "fold should call right function on Success" {
      Success(1).fold({ 2 }, { 3 }) shouldBe 3
    }

    "fold should propagate exception from Success with exception" {
      Exception().let { ex ->
        try {
          Success(1).fold({ 2 }, { throw ex })
        } catch (e: Exception) {
          ex should beTheSameInstanceAs(e)
        }
      }
    }

    "getOrDefault returns default if Failure" {
      Success(1).getOrDefault { 2 } shouldBe 1
      Failure(Exception()).getOrDefault { 2 } shouldBe 2
    }

    "getOrElse returns default if Failure" {
      val e: Throwable = Exception()

      Success(1).getOrElse { _: Throwable -> 2 } shouldBe 1
      Failure(e).getOrElse { (it shouldEqual e); 2 } shouldBe 2
    }

    "orNull returns null if Failure" {
      Success(1).orNull() shouldBe 1

      val e: Throwable = Exception()
      val failure1: Try<Int> = Failure(e)
      failure1.orNull() shouldBe null
    }

    "recoverWith should modify Failure entity" {
      Success(1).recoverWith { Failure(Exception()) } shouldBe Success(1)
      Success(1).recoverWith { Success(2) } shouldBe Success(1)
      Failure(Exception()).recoverWith { Success(2) } shouldBe Success(2)
    }

    "recover should modify Failure value" {
      Success(1).recover { 2 } shouldBe Success(1)
      Failure(Exception()).recover { 2 } shouldBe Success(2)
    }

    "toEither with onLeft should return Either.Right with correct right value if Try is Success" {
      Success(1).toEither { "myDomainError" } shouldBe 1.right()
    }

    "toEither with onLeft should return Either.Left with correct left value if Try is Failure" {
      Failure(Exception()).toEither { "myDomainError" } shouldBe "myDomainError".left()
    }

    "Cartesian builder should build products over homogeneous Try" {
      map(
        Success("11th"),
        Success("Doctor"),
        Success("Who")
      ) { (a, b, c) -> "$a $b $c" } shouldBe Success("11th Doctor Who")
    }

    "Cartesian builder should build products over heterogeneous Try" {
      map(
        Success(13),
        Success("Doctor"),
        Success(false)
      ) { (a, b, c) -> "${a}th $b is $c" } shouldBe Success("13th Doctor is false")
    }

    data class DoctorNotFoundException(val msg: String) : Exception()

    "Cartesian builder should build products over Failure Try" {
      map(
        Success(13),
        Failure(DoctorNotFoundException("13th Doctor is coming!")),
        Success("Who")
      ) { (a, b, @Suppress("UNUSED_DESTRUCTURED_PARAMETER_ENTRY") c) ->
        @Suppress("UNREACHABLE_CODE") "${a}th $b is $c"
      } shouldBe Failure(DoctorNotFoundException("13th Doctor is coming!"))
    }

    "show" {
      val problem = success.flatMap { x -> failure.map { y -> x / y } }
      when (problem) {
        is Success -> fail("This should not be possible")
        is Failure -> {
          // Success
        }
      }
    }

    "getOrElse" {
      success.getOrElse { 5 } shouldBe 10
      failure.getOrElse { 5 } shouldBe 5
    }

    "orElse" {
      success.orElse { Success(5) } shouldBe Success(10)
      failure.orElse { Success(5) } shouldBe Success(5)
    }

    "flatMap" {
      success.flatMap { Success(it * 2) } shouldBe Success(20)
      (failure.flatMap { Success(it * 2) }.isFailure()) shouldBe true
    }

    "map" {
      success.map { it * 2 } shouldBe Success(20)
      (failure.map { it * 2 }.isFailure()) shouldBe true
    }

    "exists" {
      (success.exists { it > 5 }) shouldBe true
      (failure.exists { it > 5 }) shouldBe false
    }

    "filter" {
      (success.filter { it > 5 }.isSuccess()) shouldBe true
      (success.filter { it < 5 }.isFailure()) shouldBe true
      (failure.filter { it > 5 }.isSuccess()) shouldBe false
    }

    "toOption" {
      (success.toOption().isDefined()) shouldBe true
      (failure.toOption().isEmpty()) shouldBe true
    }

    "success" {
      10.success() shouldBe success
    }

    "failure" {
      val ex = NumberFormatException()
      ex.failure<Int>() shouldBe Failure(ex)
    }

    "flatten" {
      (Try { success }.flatten().isSuccess()) shouldBe true
      (Try { failure }.flatten().isFailure()) shouldBe true
      (Try<Try<Int>> { throw RuntimeException("") }.flatten().isFailure()) shouldBe true
    }

  }
}
