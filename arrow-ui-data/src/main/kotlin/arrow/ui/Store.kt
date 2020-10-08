package arrow.ui

import arrow.core.ForId
import arrow.core.Id
import arrow.core.extensions.id.applicative.applicative
import arrow.core.extensions.id.comonad.comonad
import arrow.core.extensions.id.functor.functor
import arrow.core.fix
import arrow.typeclasses.Monoid

typealias ForStore = ForStoreT

typealias StoreOf<S, A> = StoreTOf<S, ForId, A>

typealias StorePartialOf<S> = StoreTPartialOf<S, ForId>

typealias Store<S, A> = StoreT<S, ForId, A>

fun <S, A> Store(state: S, render: (S) -> A): Store<S, A> = StoreT(state, Id(render))

fun <S, A, B> Store<S, A>.map(f: (A) -> B): Store<S, B> = map(Id.functor(), f)

fun <S, A, B> Store<S, A>.ap(MS: Monoid<S>, ff: Store<S, (A) -> B>): Store<S, B> = ap(Id.applicative(), MS, ff)

fun <S, A, B> Store<S, A>.coflatMap(f: (Store<S, A>) -> B): Store<S, B> = coflatMap(Id.comonad(), f)

fun <S, A> Store<S, A>.extract(): A = extract(Id.comonad())

fun <S, A> Store<S, A>.duplicate(): Store<S, Store<S, A>> = duplicate(Id.comonad())

fun <S, A> Store<S, A>.peek(state: S): A = peek(Id.comonad(), state)

fun <S, A> Store<S, A>.lower(): Id<A> = lower(Id.functor()).fix()

fun <S, A> Store<S, A>.move(newState: S): Store<S, A> = move(Id.comonad(), newState)

fun Store(): StoreApi = StoreApi

object StoreApi {
  fun <S, A> just(MS: Monoid<S>, a: A): Store<S, A> = StoreT.just(Id.applicative(), MS, a)
}
