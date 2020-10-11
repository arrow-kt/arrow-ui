package arrow.ui.extensions

import arrow.typeclasses.Applicative
import arrow.typeclasses.Functor
import arrow.typeclasses.Monad
import arrow.typeclasses.MonadSyntax
import arrow.ui.CoApi
import arrow.ui.CoT
import arrow.ui.Moore
import arrow.ui.MooreAction
import arrow.ui.MooreActionApi
import arrow.ui.MooreActionPartialOf
import arrow.ui.extensions.cot.monad.monad
import arrow.ui.extensions.moore.comonad.comonad

fun <W, A> MooreActionApi.fx(c: suspend MonadSyntax<MooreActionPartialOf<W>>.() -> A): MooreAction<W, A> =
  CoApi.fx(Moore.comonad(), c)

fun <W> MooreActionApi.functor(): Functor<MooreActionPartialOf<W>> = CoApi.functor()

fun <W> MooreActionApi.applicative(): Applicative<MooreActionPartialOf<W>> = CoApi.applicative(Moore.comonad())

fun <W> MooreActionApi.monad(): Monad<MooreActionPartialOf<W>> = CoT.monad(Moore.comonad())
