package arrow.data

import arrow.core.Eval
import arrow.higherkind

@higherkind
data class SetK<out A>(val set: Set<A>) : SetKOf<A>, Set<A> by set {

    fun <B> foldLeft(b: B, f: (B, A) -> B): B = fold(b, f)

    fun <B> foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> {
        fun loop(fa_p: SetK<A>): Eval<B> = when {
            fa_p.set.isEmpty() -> lb
            else -> f(fa_p.set.first(), Eval.defer { loop(fa_p.set.drop(1).toSet().k()) })
        }
        return Eval.defer { loop(this) }
    }

    companion object {

        fun <A> just(a: A): SetK<A> = setOf(a).k()

        fun empty(): SetK<Nothing> = empty

        private val empty = emptySet<Nothing>().k()

    }
}

fun <A> SetKOf<A>.combineK(y: SetKOf<A>): SetK<A> = (fix().set + y.fix().set).k()

fun <A> Set<A>.k(): SetK<A> = SetK(this)
