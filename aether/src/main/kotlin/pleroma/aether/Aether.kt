package pleroma.aether

import kotlin.math.max

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
typealias Env = Map<String, Term>

fun shift(
    term: Term,
    v: Int,
    i: Int = 0,
): Term =
    when (term) {
        is Term.Var -> if (term.i >= i) Term.Var(term.i + v) else term
        is Term.Pi -> Term.Pi(shift(term.t, v, i), shift(term.body, v, i + 1))
        is Term.Abs -> Term.Abs(shift(term.t, v, i), shift(term.body, v, i + 1))
        is Term.App -> Term.App(shift(term.f, v, i), shift(term.arg, v, i))
        else -> term
    }

fun subst(
    term: Term,
    i: Int,
    s: Term,
): Term =
    when (term) {
        is Term.Var ->
            when {
                term.i == i -> s
                term.i > i -> Term.Var(term.i - 1)
                else -> term
            }
        is Term.Pi -> Term.Pi(subst(term.t, i, s), subst(term.body, i + 1, shift(s, 1)))
        is Term.Abs -> Term.Abs(subst(term.t, i, s), subst(term.body, i + 1, shift(s, 1)))
        is Term.App -> Term.App(subst(term.f, i, s), subst(term.arg, i, s))
        else -> term
    }

fun whnf(
    env: Env,
    term: Term,
): Term {
    println("[whnf] term -> " + term)
    return when (term) {
        is Term.Const -> {
            val freevar = env[term.name]
            if (freevar != null) whnf(env, freevar) else term
        }

        is Term.App -> {
            val f = whnf(env, term.f)
            if (f is Term.Abs) {
                println("[whnf] f.body -> " + f.body)
                println("[whnf] term.arg -> " + term.arg)
                println("[whnf] r -> " + subst(f.body, 0, shift(term.arg, 1)))
                whnf(env, subst(f.body, 0, shift(term.arg, 1)))
            } else {
                Term.App(f, term.arg)
            }
        }

        else -> term
    }
}

fun normalize(
    env: Env,
    term: Term,
): Term {
    return when (term) {
        is Term.App -> {
            val t = whnf(env, term)
            if (t is Term.App) {
                Term.App(normalize(env, t.f), normalize(env, t.arg))
            } else {
                normalize(env, t)
            }
        }
        is Term.Pi -> Term.Pi(normalize(env, term.t), normalize(env, term.body))
        is Term.Abs -> Term.Abs(normalize(env, term.t), normalize(env, term.body))
        else -> term
    }
}

fun cmp(
    env: Env,
    a: Term,
    b: Term,
): Boolean {
    println("[cmp] a -> " + a)
    println("[cmp] b -> " + b)
    println("[cmp] normalize(env, a) -> " + normalize(env, a))
    println("[cmp] normalize(env, b) -> " + normalize(env, b))
    println("[cmp] normalize(env, a) == normalize(env, b) -> " + (normalize(env, a) == normalize(env, b)))
    return normalize(env, a) == normalize(env, b)
}

fun typeOf(
    env: Env,
    ctx: Ctx,
    term: Term,
): Term =
    when (term) {
        is Term.Num -> Term.Const("number")
        is Term.Str -> Term.Const("string")

        is Term.Sort -> Term.Sort(term.uni + 1)
        is Term.Var -> ctx[ctx.lastIndex - term.i]

        is Term.Const -> {
            val freevar = env[term.name]
            if (freevar == null) throw RuntimeException("Unknown constant: ${term.name}")
            freevar
        }

        is Term.Pi -> {
            val t = typeOf(env, ctx, term.t)
            if (t !is Term.Sort) {
                throw RuntimeException("Pi parameter is not a type")
            }

            val body = typeOf(env, ctx + term.t, term.body)

            if (body !is Term.Sort) {
                throw RuntimeException("Pi body is not a type")
            }

            Term.Sort(max(t.uni, body.uni))
        }

        is Term.Abs -> {
            val t = typeOf(env, ctx, term.t)
            if (t !is Term.Sort) {
                throw RuntimeException("Lambda parameter type is not a type")
            }

            val body = typeOf(env, ctx + term.t, term.body)

            Term.Pi(term.t, body)
        }

        is Term.App -> {
            println("[typeof-app] term -> " + term)
            
            val f = typeOf(env, ctx, term.f)

            println("[typeof-app] f -> " + f)

            if (f !is Term.Pi) {
                throw RuntimeException("Applying non-function")
            }

            val arg = typeOf(env, ctx, term.arg)

            println("[typeof-app] (arg) -> " + arg)

            println("[typeof-app] (whnf(env, f.t)) -> " + whnf(env, f.t))
            println("[typeof-app] (normalize(env, f.t)) -> " + normalize(env, f.t))

            if (!cmp(env, arg, f.t)) {
                throw RuntimeException("Argument type mismatch: $arg != " + f.t)
            }

            normalize(env, subst(f.body, 0, shift(term.arg, 1)))
        }
    }
