package arrow.data

import arrow.core.*
import arrow.instances.*
import arrow.instances.eq
import arrow.instances.extensions
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

    val EQ = Try.eq(Eq { a, b -> a::class == b::class }, Int.eq())

    ForTry extensions {
      testLaws(
        SemigroupLaws.laws(Try.semigroup(Int.semigroup()), Try.just(1), Try.just(2), Try.just(3), EQ),
        MonoidLaws.laws(Try.monoid(MO = Int.monoid()), Try.just(1), EQ),
        EqLaws.laws(EQ) { Try.just(it) },
        ShowLaws.laws(Try.show(), EQ) { Try.just(it) },
        MonadErrorLaws.laws(this, Eq.any(), Eq.any()),
        TraverseLaws.laws(this, this, ::Success, Eq.any())
      )
    }

    "empty should return a Success of the empty of the inner type" {
      forAll { a: String ->
        Success(String.monoid().run { empty() }) == Try.monoid(String.monoid()).run { empty() }
      }
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
      Try.invoke { throw ex } shouldBe Failure<Any>(ex)
    }

    "filter evaluates predicate" {
      val failure: Try<Int> = Failure(Exception())

      Success(1).filter { true } shouldBe Success(1)
      Success(1).filter { false } shouldBe Failure<Int>(TryException.PredicateException("Predicate does not hold for 1"))
      failure.filter { true } shouldBe failure
      failure.filter { false } shouldBe failure
    }

    "failed tries to swap" {
      val ex = Exception()
      val failure: Try<Int> = Failure(ex)

      Success(1).failed() shouldBe Failure<Int>(TryException.UnsupportedOperationException("Success"))
      failure.failed() shouldBe Success(ex)
    }

    "fold should call left function on Failure" {
      Failure<Int>(Exception()).fold({ 2 }, { 3 }) shouldBe 2
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
      Failure<Int>(Exception()).getOrDefault { 2 } shouldBe 2
    }

    "getOrElse returns default if Failure" {
      val e: Throwable = Exception()

      Success(1).getOrElse { _: Throwable -> 2 } shouldBe 1
      Failure<Int>(e).getOrElse { (it shouldEqual e); 2 } shouldBe 2
    }

    "orNull returns null if Failure" {
      val e: Throwable = Exception()

      Success(1).orNull() shouldBe 1
      Failure<Int>(e).orNull() shouldBe null
    }

    "recoverWith should modify Failure entity" {
      Success(1).recoverWith { Failure<Int>(Exception()) } shouldBe Success(1)
      Success(1).recoverWith { Success(2) } shouldBe Success(1)
      Failure<Int>(Exception()).recoverWith { Success(2) } shouldBe Success(2)
    }

    "recover should modify Failure value" {
      Success(1).recover { 2 } shouldBe Success(1)
      Failure<Int>(Exception()).recover { 2 } shouldBe Success(2)
    }

    "transform applies left function for Success" {
      Success(1).transform({ Success(2) }, { Success(3) }) shouldBe Success(2)
    }

    "transform applies right function for Failure" {
      Failure<Int>(Exception()).transform({ Success(2) }, { Success(3) }) shouldBe Success(3)
    }

    "Cartesian builder should build products over homogeneous Try" {
      ForTry extensions {
        map(
          Success("11th"),
          Success("Doctor"),
          Success("Who"),
          { (a, b, c) -> "$a $b $c" }) shouldBe Success("11th Doctor Who")
      }
    }

    "Cartesian builder should build products over heterogeneous Try" {
      ForTry extensions {
        map(
          Success(13),
          Success("Doctor"),
          Success(false),
          { (a, b, c) -> "${a}th $b is $c" }) shouldBe Success("13th Doctor is false")
      }
    }

    data class DoctorNotFoundException(val msg: String) : Exception()

    "Cartesian builder should build products over Failure Try" {
      ForTry extensions {
        map(
          Success(13),
          Failure<Boolean>(DoctorNotFoundException("13th Doctor is coming!")),
          Success("Who"),
          { (a, b, c) -> "${a}th $b is $c" }) shouldBe Failure<String>(DoctorNotFoundException("13th Doctor is coming!"))
      }
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

    "get" {
      10 shouldBe success()
      try {
        failure()
        fail("")
      } catch (e: Exception) {
        (e is NumberFormatException) shouldBe true
      }
    }

    "getOrElse" {
      success.getOrElse { 5 } shouldBe 10
      failure.getOrElse { 5 } shouldBe 5
    }

    "orElse" {
      success.orElse { Success(5) }.get() shouldBe 10
      failure.orElse { Success(5) }.get() shouldBe 5
    }

    "`foreach with side effect (applied on Success)`" {
      var wasInside = false
      success.foreach { wasInside = true }
      wasInside shouldBe true
    }

    "`foreach with side effect (applied on Failure)`" {
      var wasInside = false
      failure.foreach { wasInside = true }
      wasInside shouldBe false
    }

    "`foreach with exception thrown inside (applied on Success)`" {
      try {
        success.foreach { throw RuntimeException("thrown inside") }
      } catch (e: Throwable) {
        e.message shouldBe "thrown inside"
      }
    }

    "`foreach with exception thrown inside (applied on Failure)`" {
      failure.foreach { throw RuntimeException("thrown inside") }
      // and no exception should be thrown
    }

    "`onEach with side effect (applied on Success)`" {
      var wasInside = false
      success.onEach { wasInside = true }
      wasInside shouldBe true
    }

    "`onEach with side effect (applied on Failure)`" {
      var wasInside = false
      failure.onEach { wasInside = true }
      wasInside shouldBe false
    }

    "`onEach with exception thrown inside (applied on Success)`" {
      try {
        success.onEach { throw RuntimeException("thrown inside") }.get()
      } catch (e: Throwable) {
        e.message shouldBe "thrown inside"
      }
    }

    "`onEach with exception thrown inside (applied on Failure)`" {
      try {
        failure.onEach { throw RuntimeException("thrown inside") }.get()
      } catch (e: Throwable) {
        e.javaClass shouldBe NumberFormatException::class.java
      }
    }

    "`onEach with change of carried value (applied on Success)`" {
      val result = success.onEach { it * 2 }.get()
      result shouldBe 10
    }

    "`onEach with change of carried value (applied on Failure)`" {
      try {
        failure.onEach { it * 2 }.get()
      } catch (e: Throwable) {
        e.javaClass shouldBe NumberFormatException::class.java
      }
    }

    "flatMap" {
      success.flatMap { Success(it * 2) }.get() shouldBe 20
      (failure.flatMap { Success(it * 2) }.isFailure()) shouldBe true
    }

    "map" {
      success.map { it * 2 }.get() shouldBe 20
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

    "rescue" {
      success.rescue { Success(5) }.get() shouldBe 10
      failure.rescue { Success(5) }.get() shouldBe 5
    }

    "handle" {
      success.handle { 5 }.get() shouldBe 10
      failure.handle { 5 }.get() shouldBe 5
    }

    "onSuccessAndOnFailure" {
      success.onSuccess { it shouldBe 10 }
        .onFailure { fail("") }
      failure.onSuccess { fail("") }
        .onFailure { }
    }

    "toOption" {
      (success.toOption().isDefined()) shouldBe true
      (failure.toOption().isEmpty()) shouldBe true
    }

    "toDisjunction" {
      (success.toDisjunction().isRight()) shouldBe true
      (failure.toDisjunction().isLeft()) shouldBe true
    }

    "failed" {
      success.failed().onSuccess { (it is UnsupportedOperationException) shouldBe true }
      failure.failed().onSuccess { (it is NumberFormatException) shouldBe true }
    }

    "transform" {
      success.transform({ Try { it.toString() } }) { Try { "NaN" } }.get() shouldBe "10"
      failure.transform({ Try { it.toString() } }) { Try { "NaN" } }.get() shouldBe "NaN"
    }

    "flatten" {
      (Try { success }.flatten().isSuccess()) shouldBe true
      (Try { failure }.flatten().isFailure()) shouldBe true
      (Try<Try<Int>> { throw RuntimeException("") }.flatten().isFailure()) shouldBe true
    }

  }
}
