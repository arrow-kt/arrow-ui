package arrow.ui

import arrow.core.ForId
import arrow.core.Id
import arrow.core.extensions.id.applicative.applicative
import arrow.core.extensions.id.comonad.comonad
import arrow.core.extensions.id.functor.functor
import arrow.core.fix
import arrow.typeclasses.Comonad
import arrow.typeclasses.Monoid

typealias ForStore = ForStoreT

typealias StoreOf<S, A> = StoreTOf<S, ForId, A>

typealias StorePartialOf<S> = StoreTPartialOf<S, ForId>

/**
 * [Store] is equivalent to [StoreT], with the base [Comonad] being [Id]
 */
typealias Store<S, A> = StoreT<S, ForId, A>

/**
 * Initializes a [Store]
 *
 * @param state current position in space [S]
 * @param render the computation that returns the value for the current position
 */
fun <S, A> Store(state: S, render: (S) -> A): Store<S, A> = StoreT(state, Id(render))

/**
 * Map current value [A] given function [f]
 *
 * @param f the function to apply
 */
fun <S, A, B> Store<S, A>.map(f: (A) -> B): Store<S, B> = map(Id.functor(), f)

/**
 * Apply a function `(S) -> B` that operates withing the [Store] context
 *
 * @param MS [Monoid] for combining positions in space [S]
 * @param ff function with the [Store] context
 */
fun <S, A, B> Store<S, A>.ap(MS: Monoid<S>, ff: Store<S, (A) -> B>): Store<S, B> = ap(Id.applicative(), MS, ff)

/**
 * Map the value [B] from another [Store] object for the same space [S] and rebuild the structure.
 *
 * @param f the function to apply
 */
fun <S, A, B> Store<S, A>.coflatMap(f: (Store<S, A>) -> B): Store<S, B> = coflatMap(Id.comonad(), f)

/**
 * Extract the [A] value from the current position in space [S]
 */
fun <S, A> Store<S, A>.extract(): A = extract(Id.comonad())

/**
 * Unfolds the space [S] based on the current position
 */
fun <S, A> Store<S, A>.duplicate(): Store<S, Store<S, A>> = duplicate(Id.comonad())

/**
 * Computes the value [A] for position [state]
 *
 * @param state a position in space [S]
 */
fun <S, A> Store<S, A>.peek(state: S): A = peek(Id.comonad(), state)

/**
 * Obtains the comonadic value, removing the [Store] support
 */
fun <S, A> Store<S, A>.lower(): Id<A> = lower(Id.functor()).fix()

/**
 * Move the [Store] into another position
 *
 * @param newState new position for the [Store]
 */
fun <S, A> Store<S, A>.move(newState: S): Store<S, A> = move(Id.comonad(), newState)

fun Store(): StoreApi = StoreApi

object StoreApi {
  fun <S, A> just(MS: Monoid<S>, a: A): Store<S, A> = StoreT.just(Id.applicative(), MS, a)
}
