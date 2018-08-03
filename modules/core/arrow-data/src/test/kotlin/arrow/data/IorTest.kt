package arrow.data

import arrow.Kind2
import arrow.core.Either
import arrow.core.None
import arrow.core.Some
import arrow.data.Ior.Right
import arrow.instances.ForIor
import arrow.instances.eq
import arrow.instances.semigroup
import arrow.test.UnitSpec
import arrow.test.laws.*
import arrow.typeclasses.Eq
import arrow.typeclasses.Monad
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.matchers.shouldBe
import io.kotlintest.properties.forAll
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class IorTest : UnitSpec() {

  init {

    val intIorMonad: Monad<IorPartialOf<Int>> = Ior.monad(Int.semigroup())

    val EQ = Ior.eq(Eq.any(), Eq.any())

    val EQ2: Eq<Kind2<ForIor, Int, Int>> = Eq { a, b ->
      a.fix() == b.fix()
    }

    ForIor(Int.semigroup()) extensions {
      testLaws(
        BifunctorLaws.laws(Ior.bifunctor(), { Ior.Both(it, it) }, EQ2),
        EqLaws.laws(EQ, { Right(it) }),
        ShowLaws.laws(Ior.show(), EQ) { Right(it) },
        MonadLaws.laws(this, Eq.any()),
        TraverseLaws.laws(Ior.traverse(), this, ::Right, Eq.any())
      )
    }

    "bimap() should allow modify both value" {
      forAll { a: Int, b: String ->
        Ior.Right.invoke(b).bimap({ "5" }, { a * 2 }) == Ior.Right.invoke(a * 2) &&
          Ior.Left<Int, String>(a).bimap({ a * 3 }, { "5" }) == Ior.Left<Int, String>(a * 3) &&
          Ior.Both(a, b).bimap({ 2 }, { "power of $it" }) == Ior.Both(2, "power of $b")
      }
    }

    "mapLeft() should modify only left value" {
      forAll { a: Int, b: String ->
        Ior.Right<Int, String>(b).mapLeft { a * 2 } == Ior.Right<Int, String>(b) &&
          Ior.Left<Int, String>(a).mapLeft { b } == Ior.Left<String, String>(b) &&
          Ior.Both(a, b).mapLeft { "power of $it" } == Ior.Both("power of $a", b)
      }
    }

    "swap() should interchange value" {
      forAll { a: Int, b: String ->
        Ior.Both(a, b).swap() == Ior.Both(b, a)
      }
    }

    "swap() should interchange entity" {
      forAll { a: Int ->
        Ior.Left<Int, String>(a).swap() == Ior.Right<String, Int>(a) &&
          Ior.Right<String, Int>(a).swap() == Ior.Left<Int, String>(a)
      }
    }

    "unwrap() should return the isomorphic either" {
      forAll { a: Int, b: String ->
        Ior.Left<Int, String>(a).unwrap() == Either.Left(Either.Left(a)) &&
          Ior.Right<Int, String>(b).unwrap() == Either.Left(Either.Right(b)) &&
          Ior.Both(a, b).unwrap() == Either.Right(Pair(a, b))
      }
    }

    "pad() should return the correct Pair of Options" {
      forAll { a: Int, b: String ->
        Ior.Left<Int, String>(a).pad() == Pair(Some(a), None) &&
          Ior.Right<Int, String>(b).pad() == Pair(None, Some(b)) &&
          Ior.Both(a, b).pad() == Pair(Some(a), Some(b))
      }
    }

    "toEither() should convert values into a valid Either" {
      forAll { a: Int, b: String ->
        Ior.Left<Int, String>(a).toEither() == Either.Left(a) &&
          Ior.Right<Int, String>(b).toEither() == Either.Right(b) &&
          Ior.Both(a, b).toEither() == Either.Right(b)
      }
    }

    "toOption() should convert values into a valid Option" {
      forAll { a: Int, b: String ->
        Ior.Left<Int, String>(a).toOption() == None &&
          Ior.Right<Int, String>(b).toOption() == Some(b) &&
          Ior.Both(a, b).toOption() == Some(b)
      }
    }

    "toValidated() should convert values into a valid Validated" {
      forAll { a: Int, b: String ->
        Ior.Left<Int, String>(a).toValidated() == Invalid(a) &&
          Ior.Right<Int, String>(b).toValidated() == Valid(b) &&
          Ior.Both(a, b).toValidated() == Valid(b)
      }
    }

    "fromOptions() should build a correct Option<Ior>" {
      forAll { a: Int, b: String ->
        Ior.fromOptions(Some(a), None) == Some(Ior.Left.invoke(a)) &&
          Ior.fromOptions(Some(a), Some(b)) == Some(Ior.Both(a, b)) &&
          Ior.fromOptions(None, Some(b)) == Some(Ior.Right.invoke(b)) &&
          Ior.fromOptions(None, None) == None
      }
    }


    "getOrElse() should return value" {
      forAll { a: Int, b: Int ->
        Ior.Right<Int, Int>(a).getOrElse { b } == a &&
          Ior.Left<Int, Int>(a).getOrElse { b } == b &&
          Ior.Both(a, b).getOrElse { a * 2 } == b
      }

    }

    "Ior.monad.flatMap should combine left values" {
      val ior1 = Ior.Both(3, "Hello, world!")
      val iorResult = intIorMonad.run { ior1.flatMap({ Ior.Left<Int, String>(7) }) }
      iorResult shouldBe Ior.Left<Int, String>(10)
    }

  }
}
