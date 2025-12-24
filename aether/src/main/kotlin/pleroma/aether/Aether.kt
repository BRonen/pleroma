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

data class EnvEntry(val t: Term, val v: Term? = null)

typealias Env = Map<String, EnvEntry>

fun shift(
    term: Term,
    d: Int,
    cutoff: Int = 0,
): Term =
    when (term) {
        is Term.Var -> if (term.i >= cutoff) Term.Var(term.i + d) else term
        is Term.Pi -> Term.Pi(shift(term.t, d, cutoff), shift(term.body, d, cutoff + 1))
        is Term.Abs -> Term.Abs(shift(term.t, d, cutoff), shift(term.body, d, cutoff + 1))
        is Term.App -> Term.App(shift(term.f, d, cutoff), shift(term.arg, d, cutoff))
        else -> term
    }

fun subst(
    term: Term,
    j: Int,
    r: Term,
): Term =
    when (term) {
        is Term.Var ->
            if (term.i == j) {
                r
            } else if (term.i > j) {
                Term.Var(term.i - 1)
            } else {
                term
            }
        is Term.Pi -> Term.Pi(subst(term.t, j, r), subst(term.body, j + 1, shift(r, 1)))
        is Term.Abs -> Term.Abs(subst(term.t, j, r), subst(term.body, j + 1, shift(r, 1)))
        is Term.App -> Term.App(subst(term.f, j, r), subst(term.arg, j, r))
        else -> term
    }

fun normalize(
    env: Env,
    term: Term,
): Term =
    when (term) {
        is Term.App -> {
            val f = normalize(env, term.f)
            val arg = normalize(env, term.arg)
            if (f is Term.Abs) normalize(env, subst(f.body, 0, arg)) else Term.App(f, arg)
        }
        is Term.Const -> {
            val d = env[term.name]
            if (d?.v != null) normalize(env, d.v) else term
        }
        is Term.Pi -> Term.Pi(normalize(env, term.t), normalize(env, term.body))
        is Term.Abs -> Term.Abs(normalize(env, term.t), normalize(env, term.body))
        else -> term
    }

fun cmp(
    env: Env,
    a: Term,
    b: Term,
): Boolean = normalize(env, a) == normalize(env, b)

fun typeOf(
    env: Env,
    ctx: Ctx,
    term: Term,
): Term =
    when (term) {
        is Term.Num -> Term.Const("number")
        is Term.Str -> Term.Const("string")

        is Term.Sort -> Term.Sort(term.uni + 1)
        is Term.Var -> shift(ctx[ctx.lastIndex - term.i], term.i + 1)

        is Term.Const -> {
            val d = env[term.name]
            if (d != null) d.t else term
        }

        is Term.Pi -> {
            val t = normalize(env, typeOf(env, ctx, term.t))
            if (t !is Term.Sort) throw RuntimeException("Pi parameter is not a type")

            val body = normalize(env, typeOf(env, ctx + term.t, term.body))
            if (body !is Term.Sort) throw RuntimeException("Pi body is not a type")

            Term.Sort(max(t.uni, body.uni))
        }

        is Term.Abs -> {
            val t = normalize(env, typeOf(env, ctx, term.t))
            if (t !is Term.Sort) throw RuntimeException("Lambda parameter type is not a type: $t -> " + term.t)

            val body = typeOf(env, ctx + term.t, term.body)

            Term.Pi(term.t, body)
        }

        is Term.App -> {
            val f = normalize(env, typeOf(env, ctx, term.f))
            if (f !is Term.Pi) throw RuntimeException("Applying non-function")

            val arg = typeOf(env, ctx, term.arg)
            if (!cmp(env, arg, f.t)) throw RuntimeException("Argument type mismatch: $arg != " + f.t)

            normalize(env, subst(f.body, 0, term.arg))
        }
    }
