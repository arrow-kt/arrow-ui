package arrow.data

import arrow.Kind
import arrow.core.*
import arrow.higherkind
import arrow.typeclasses.Applicative
import arrow.typeclasses.Functor
import arrow.typeclasses.Monad
import arrow.typeclasses.SemigroupK

/**
 * Alias that represent stateful computation of the form `(S) -> Tuple2<S, A>` with a result in certain context `F`.
 */
typealias StateTFun<F, S, A> = (S) -> Kind<F, Tuple2<S, A>>

/**
 * Alias that represents wrapped stateful computation in context `F`.
 */
typealias StateTFunOf<F, S, A> = Kind<F, StateTFun<F, S, A>>

/**
 * Run the stateful computation within the context `F`.
 *
 * @param MF [Monad] for the context [F]
 * @param s initial state to run stateful computation
 */
fun <F, S, A> StateTOf<F, S, A>.runM(MF: Monad<F>, initial: S): Kind<F, Tuple2<S, A>> = fix().run(MF, initial)

/**
 * `StateT<F, S, A>` is a stateful computation within a context `F` yielding
 * a value of type `A`. i.e. StateT<EitherPartialOf<E>, S, A> = Either<E, State<S, A>>
 *
 * @param F the context that wraps the stateful computation.
 * @param S the state we are preforming computation upon.
 * @param A current value of computation.
 * @param runF the stateful computation that is wrapped and managed by `StateT`
 */
@higherkind
class StateT<F, S, A>(
  val runF: StateTFunOf<F, S, A>
) : StateTOf<F, S, A>, StateTKindedJ<F, S, A> {

  companion object {

    fun <F, S, T> just(MF: Monad<F>, t: T): StateT<F, S, T> =
      StateT(MF) { s -> MF.just(s toT t) }

    /**
     * Constructor to create `StateT<F, S, A>` given a [StateTFun].
     *
     * @param MF [Monad] for the context [F].
     * @param run the stateful function to wrap with [StateT].
     */
    operator fun <F, S, A> invoke(MF: Monad<F>, run: StateTFun<F, S, A>): StateT<F, S, A> = MF.run {
      StateT(just(run))
    }

    /**
     * Alias for constructor [StateT].
     *
     * @param runF the function to wrap within [StateT].
     */
    fun <F, S, A> invokeF(runF: StateTFunOf<F, S, A>): StateT<F, S, A> = StateT(runF)

    /**
     * Lift a value of type `A` into `StateT<F, S, A>`.
     *
     * @param MF [Monad] for the context [F].
     * @param fa the value to lift.
     */
    fun <F, S, A> lift(MF: Monad<F>, fa: Kind<F, A>): StateT<F, S, A> = MF.run {
      StateT(just({ s -> fa.map { a -> Tuple2(s, a) } }))
    }

    /**
     * Return input without modifying it.
     *
     * @param AF [Applicative] for the context [F].
     */
    fun <F, S> get(AF: Applicative<F>): StateT<F, S, S> =
      StateT(AF.just({ s -> AF.just(Tuple2(s, s)) }))

    /**
     * Inspect a value of the state [S] with [f] `(S) -> T` without modifying the state.
     *
     *
     *
     * @param AF [Applicative] for the context [F].
     * @param f the function applied to inspect [T] from [S].
     */
    fun <F, S, T> inspect(AF: Applicative<F>, f: (S) -> T): StateT<F, S, T> =
      StateT(AF.just({ s -> AF.just(Tuple2(s, f(s))) }))

    /**
     * Modify the state with [f] `(S) -> S` and return [Unit].
     *
     * @param AF [Applicative] for the context [F].
     * @param f the modify function to apply.
     */
    fun <F, S> modify(AF: Applicative<F>, f: (S) -> S): StateT<F, S, Unit> =
      StateT(AF.just({ s ->
        val just = AF.just(f(s))
        AF.run {
          just.map { Tuple2(it, Unit) }
        }
      }))

    /**
     * Modify the state with an [Applicative] function [f] `(S) -> Kind<F, S>` and return [Unit].
     *
     * @param AF [Applicative] for the context [F].
     * @param f the modify function to apply.
     */
    fun <F, S> modifyF(AF: Applicative<F>, f: (S) -> Kind<F, S>): StateT<F, S, Unit> =
      StateT(AF.just({ s -> AF.run { f(s).map { Tuple2(it, Unit) } } }))

    /**
     * Set the state to a value [s] and return [Unit].
     *
     * @param AF [Applicative] for the context [F].
     * @param s value to set.
     */
    fun <F, S> set(AF: Applicative<F>, s: S): StateT<F, S, Unit> =
      StateT(AF.just({ _ -> AF.just(Tuple2(s, Unit)) }))

    /**
     * Set the state to a value [s] of type `Kind<F, S>` and return [Unit].
     *
     * @param AF [Applicative] for the context [F].
     * @param s value to set.
     */
    fun <F, S> setF(AF: Applicative<F>, s: Kind<F, S>): StateT<F, S, Unit> =
      StateT(AF.just({ _ -> AF.run { s.map { Tuple2(it, Unit) } } }))

    /**
     * Tail recursive function that keeps calling [f]  until [arrow.Either.Right] is returned.
     *
     * @param a initial value to start running recursive call to [f]
     * @param f function that is called recusively until [arrow.Either.Right] is returned.
     * @param MF [Monad] for the context [F].
     */
    fun <F, S, A, B> tailRecM(MF: Monad<F>, a: A, f: (A) -> Kind<StateTPartialOf<F, S>, Either<A, B>>): StateT<F, S, B> =
      StateT(MF.just({ s: S ->
        MF.tailRecM(Tuple2(s, a)) { (s, a0) ->
          MF.run {
            f(a0).runM(this, s).map { (s, ab) ->
              ab.bimap({ a1 -> Tuple2(s, a1) }, { b -> Tuple2(s, b) })
            }
          }
        }
      }))
  }

  /**
   * Map current value [A] given a function [f].
   *
   * @param f the function to apply.
   * @param FF [Functor] for the context [F].
   */
  fun <B> map(FF: Functor<F>, f: (A) -> B): StateT<F, S, B> = transform(FF) { (s, a) -> Tuple2(s, f(a)) }

  /**
   * Combine with another [StateT] of same context [F] and state [S].
   *
   * @param sb other state with value of type `B`.
   * @param f the function to apply.
   * @param MF [Monad] for the context [F].
   */
  fun <B, Z> map2(MF: Monad<F>, sb: StateT<F, S, B>, fn: (A, B) -> Z): StateT<F, S, Z> =
    MF.run {
      invokeF(runF.map2(sb.runF) { (ssa, ssb) ->
        ssa.andThen { fsa ->
          fsa.flatMap { (s, a) ->
            ssb(s).map { (s, b) -> Tuple2(s, fn(a, b)) }
          }
        }
      })
    }

  /**
   * Controlled combination of [StateT] that is of same context [F] and state [S] using [Eval].
   *
   * @param sb other state with value of type `B`.
   * @param f the function to apply.
   * @param MF [Monad] for the context [F].
   */
  fun <B, Z> map2Eval(MF: Monad<F>, sb: Eval<StateT<F, S, B>>, fn: (A, B) -> Z): Eval<StateT<F, S, Z>> = MF.run {
    runF.map2Eval(sb.map { it.runF }) { (ssa, ssb) ->
      ssa.andThen { fsa ->
        fsa.flatMap { (s, a) ->
          ssb((s)).map { (s, b) -> Tuple2(s, fn(a, b)) }
        }
      }
    }.map { invokeF(it) }
  }

  /**
   * Apply a function `(S) -> B` that operates within the [StateT] context.
   *
   * @param ff function with the [StateT] context.
   * @param MF [Monad] for the context [F].
   */
  fun <B> ap(MF: Monad<F>, ff: StateTOf<F, S, (A) -> B>): StateT<F, S, B> =
    ff.fix().map2(MF, this) { f, a -> f(a) }

  /**
   * Create a product of the value types of [StateT].
   *
   * @param sb other stateful computation.
   * @param MF [Monad] for the context [F].
   */
  fun <B> product(MF: Monad<F>, sb: StateT<F, S, B>): StateT<F, S, Tuple2<A, B>> = map2(MF, sb) { a, b -> Tuple2(a, b) }

  /**
   * Map the value [A] to another [StateT] object for the same state [S] and context [F] and flatten the structure.
   *
   * @param fas the function to apply.
   * @param MF [Monad] for the context [F].
   */
  fun <B> flatMap(MF: Monad<F>, fas: (A) -> StateTOf<F, S, B>): StateT<F, S, B> = MF.run {
    invokeF(
      runF.map { sfsa ->
        sfsa.andThen { fsa ->
          fsa.flatMap {
            fas(it.b).runM(MF, it.a)
          }
        }
      })
  }

  /**
   * Map the value [A] to a arbitrary type [B] that is within the context of [F].
   *
   * @param faf the function to apply.
   * @param MF [Monad] for the context [F].
   */
  fun <B> flatMapF(MF: Monad<F>, faf: (A) -> Kind<F, B>): StateT<F, S, B> = MF.run {
    invokeF(
      runF.map { sfsa ->
        sfsa.andThen { fsa ->
          fsa.flatMap { (s, a) ->
            faf(a).map { b -> Tuple2(s, b) }
          }
        }
      })
  }

  /**
   * Transform the product of state [S] and value [A] to an another product of state [S] and an arbitrary type [B].
   *
   * @param f the function to apply.
   * @param FF [Functor] for the context [F].
   */
  fun <B> transform(FF: Functor<F>, f: (Tuple2<S, A>) -> Tuple2<S, B>): StateT<F, S, B> = FF.run {
    invokeF(
      runF.map { sfsa ->
        sfsa.andThen { fsa ->
          fsa.map(f)
        }
      })
  }

  /**
   * Combine two [StateT] objects using an instance of [SemigroupK] for [F].
   *
   * @param y other [StateT] object to combine.
   * @param MF [Monad] for the context [F].
   * @param SF [SemigroupK] for [F].
   */
  fun combineK(MF: Monad<F>, SF: SemigroupK<F>, y: StateTOf<F, S, A>): StateT<F, S, A> = SF.run {
    StateT(MF.just({ s -> run(MF, s).combineK(y.fix().run(MF, s)) }))
  }

  /**
   * Run the stateful computation within the context `F`.
   *
   * @param s initial state to run stateful computation.
   * @param MF [Monad] for the context [F].
   */
  fun run(MF: Monad<F>, initial: S): Kind<F, Tuple2<S, A>> = MF.run {
    runF.flatMap { f -> f(initial) }
  }

  /**
   * Run the stateful computation within the context `F` and get the value [A].
   *
   * @param s initial state to run stateful computation.
   * @param MF [Monad] for the context [F].
   */
  fun runA(MF: Monad<F>, s: S): Kind<F, A> = MF.run {
    run(MF, s).map { it.b }
  }

  /**
   * Run the stateful computation within the context `F` and get the state [S].
   *
   * @param s initial state to run stateful computation.
   * @param MF [Monad] for the context [F].
   */
  fun runS(MF: Monad<F>, s: S): Kind<F, S> = MF.run {
    run(MF, s).map { it.a }
  }
}

/**
 * Wrap the function with [StateT].
 *
 * @param MF [Monad] for the context [F].
 */
fun <F, S, A> StateTFunOf<F, S, A>.stateT(): StateT<F, S, A> = StateT(this)

/**
 * Wrap the function with [StateT].
 *
 * @param MF [Monad] for the context [F].
 */
fun <F, S, A> StateTFun<F, S, A>.stateT(MF: Monad<F>): StateT<F, S, A> = StateT(MF, this)
