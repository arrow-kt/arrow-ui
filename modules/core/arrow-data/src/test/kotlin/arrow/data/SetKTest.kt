package arrow.data

import arrow.instances.IntEqInstance
import arrow.test.UnitSpec
import arrow.test.laws.*
import arrow.typeclasses.Eq
import io.kotlintest.KTestJUnitRunner
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class SetKTest : UnitSpec() {

    init {

        val EQ = SetK.eq(IntEqInstance)

        testLaws(
                EqLaws.laws(EQ) { SetK.just(it) },
                ShowLaws.laws(SetK.show(), EQ) { SetK.just(it) },
                SemigroupKLaws.laws(SetK.semigroupK(), { SetK.just(it) }, Eq.any()),
                MonoidKLaws.laws(SetK.monoidK(), { SetK.just(it) }, Eq.any()),
                FoldableLaws.laws(SetK.foldable(), { SetK.just(it) }, Eq.any())
        )
    }
}
