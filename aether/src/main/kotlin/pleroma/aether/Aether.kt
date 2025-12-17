package pleroma.aether

import kotlin.math.*

sealed class Term {
    data class Sort(val uni: Int) : Term()
    data class Const(val name: String) : Term()
    data class Num(val value: Int) : Term()
    data class Str(val value: String) : Term()
    data class Var(val i: Int) : Term()
    data class Pi(val t: Term, val body: Term) : Term()
    data class Abs(val t: Term, val body: Term) : Term()
    data class App(val f: Term, val arg: Term) : Term()
}

typealias Ctx = List<Term>

// (shift ( . ( . #2)) t) -> ( . #1)
// (shift ( . ( . #0)) t) -> ( . #0)

fun shift(term: Term, v: Int, i: Int = 0): Term =
    when (term) {
        is Term.Var -> if (term.i >= i) Term.Var(term.i + v) else term
        is Term.Pi -> Term.Pi(shift(term.t, v, i), shift(term.body, v, i + 1))
        is Term.Abs -> Term.Abs(shift(term.t, v, i), shift(term.body, v, i + 1))
        is Term.App -> Term.App(shift(term.f, v, i), shift(term.arg, v, i))
        else -> term
    }

fun subst(term: Term, i: Int, s: Term): Term =
    when (term) {
        is Term.Var ->
            when {
                term.i == i -> s
                term.i > i -> Term.Var(term.i - 1)
                else -> term
            }
        is Term.Pi -> Term.Pi(subst(term.t, i, s), subst(term.body, i + 1, shift(s, 1)))
        is Term.Abs -> Term.Abs(subst(term.t, i, s), subst(term.body, i + 1, shift(s, 1)) )
        is Term.App -> Term.App(subst(term.f, i, s), subst(term.arg, i, s))
        else -> term
    }

fun whnf(term: Term): Term =
    if (term is Term.App) {
        val f = whnf(term.f)
        if (f is Term.Abs) {
            whnf(subst(f.body, 0, shift(term.arg, 1)))
        } else {
            Term.App(f, term.arg)
        }
    } else term

fun normalize(term: Term): Term =
    when (term) {
        is Term.App -> {
            val t = whnf(term)
            if (t is Term.App)
                Term.App(normalize(t.f), normalize(t.arg))
            else
                normalize(t)
        }
        is Term.Pi -> Term.Pi(normalize(term.t), normalize(term.body))
        is Term.Abs -> Term.Abs(normalize(term.t), normalize(term.body))
        else -> term
    }

fun cmp(a: Term, b: Term): Boolean {
    return normalize(a) == normalize(b)
}

fun typeOf(ctx: Ctx, term: Term): Term =
    when (term) {
        is Term.Sort -> Term.Sort(term.uni + 1)
        is Term.Const -> Term.Sort(0)
        is Term.Num -> Term.Const("number")
        is Term.Str -> Term.Const("string")
        is Term.Var -> ctx[ctx.lastIndex - term.i]

        is Term.Pi -> {
            val t = typeOf(ctx, term.t)
            if (t !is Term.Sort)
                throw RuntimeException("Pi parameter is not a type")

            val body = typeOf(ctx + term.t, term.body)

            if (body !is Term.Sort)
                throw RuntimeException("Pi body is not a type")

            Term.Sort(max(t.uni, body.uni))
        }

        is Term.Abs -> {
            val t = typeOf(ctx, term.t)
            if (t !is Term.Sort)
                throw RuntimeException("Lambda parameter type is not a type")

            val body = typeOf(ctx + term.t, term.body)

            Term.Pi(term.t, body)
        }

        is Term.App -> {
            val f = typeOf(ctx, term.f)

            if (f !is Term.Pi)
                throw RuntimeException("Applying non-function")

            val arg = typeOf(ctx, term.arg)

            if (!cmp(arg, f.t))
                throw RuntimeException("Argument type mismatch: " + arg + " != " + f.t)

            normalize(subst(f.body, 0, shift(term.arg, 1)))
        }
    }
