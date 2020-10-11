package arrow.ui.extensions

import arrow.typeclasses.Applicative
import arrow.typeclasses.Functor
import arrow.typeclasses.Monad
import arrow.typeclasses.MonadSyntax
import arrow.ui.CoApi
import arrow.ui.CoT
import arrow.ui.Moore
import arrow.ui.MooreInput
import arrow.ui.MooreInputApi
import arrow.ui.MooreInputPartialOf
import arrow.ui.extensions.cot.monad.monad
import arrow.ui.extensions.moore.comonad.comonad

fun <W, A> MooreInputApi.fx(c: suspend MonadSyntax<MooreInputPartialOf<W>>.() -> A): MooreInput<W, A> =
  CoApi.fx(Moore.comonad(), c)

fun <W> MooreInputApi.functor(): Functor<MooreInputPartialOf<W>> = CoApi.functor()

fun <W> MooreInputApi.applicative(): Applicative<MooreInputPartialOf<W>> = CoApi.applicative(Moore.comonad())

fun <W> MooreInputApi.monad(): Monad<MooreInputPartialOf<W>> = CoT.monad(Moore.comonad())
