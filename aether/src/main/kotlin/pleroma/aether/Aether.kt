package pleroma.aether

sealed class Term {
    data class Var(val index: Int) : Term()

    object Sort : Term() {
        override fun toString() = "Sort"
    }

    data class Pi(val paramType: Term, val body: Term) : Term()

    data class Lam(val paramType: Term, val body: Term) : Term()

    data class App(val fn: Term, val arg: Term) : Term()
}


typealias Context = List<Term>

fun Context.lookup(i: Int): Term =
    this[this.size - 1 - i]

fun shift(term: Term, by: Int, cutoff: Int = 0): Term =
    when (term) {
        is Term.Var ->
            if (term.index >= cutoff)
                Term.Var(term.index + by)
            else
                term

        is Term.Sort -> Term.Sort

        is Term.Pi ->
            Term.Pi(
                shift(term.paramType, by, cutoff),
                shift(term.body, by, cutoff + 1)
            )

        is Term.Lam ->
            Term.Lam(
                shift(term.paramType, by, cutoff),
                shift(term.body, by, cutoff + 1)
            )

        is Term.App ->
            Term.App(
                shift(term.fn, by, cutoff),
                shift(term.arg, by, cutoff)
            )
    }

fun subst(term: Term, j: Int, s: Term): Term =
    when (term) {
        is Term.Var ->
            when {
                term.index == j -> s
                term.index > j -> Term.Var(term.index - 1)
                else -> term
            }

        is Term.Sort -> Term.Sort

        is Term.Pi ->
            Term.Pi(
                subst(term.paramType, j, s),
                subst(term.body, j + 1, shift(s, 1))
            )

        is Term.Lam ->
            Term.Lam(
                subst(term.paramType, j, s),
                subst(term.body, j + 1, shift(s, 1))
            )

        is Term.App ->
            Term.App(
                subst(term.fn, j, s),
                subst(term.arg, j, s)
            )
    }

fun whnf(term: Term): Term =
    when (term) {
        is Term.App -> {
            val fn = whnf(term.fn)
            if (fn is Term.Lam) {
                whnf(subst(fn.body, 0, shift(term.arg, 1)))
            } else {
                Term.App(fn, term.arg)
            }
        }
        else -> term
    }

fun normalize(term: Term): Term =
    when (term) {
        is Term.Var -> term
        is Term.Sort -> Term.Sort
        is Term.Pi ->
            Term.Pi(
                normalize(term.paramType),
                normalize(term.body)
            )
        is Term.Lam ->
            Term.Lam(
                normalize(term.paramType),
                normalize(term.body)
            )
        is Term.App -> {
            val t = whnf(term)
            if (t is Term.App)
                Term.App(normalize(t.fn), normalize(t.arg))
            else
                normalize(t)
        }
    }

fun defEq(a: Term, b: Term): Boolean {
    return normalize(a) == normalize(b)
}

fun typeOf(ctx: Context, term: Term): Term =
    when (term) {

        is Term.Sort -> Term.Sort

        is Term.Var -> ctx.lookup(term.index)

        is Term.Pi -> {
            val aType = typeOf(ctx, term.paramType)
            if (!defEq(aType, Term.Sort))
                throw RuntimeException("Pi parameter is not a type")

            val bodyType =
                typeOf(ctx + term.paramType, term.body)

            if (!defEq(bodyType, Term.Sort))
                throw RuntimeException("Pi body is not a type")

            Term.Sort
        }

        is Term.Lam -> {
            val paramTypeType = typeOf(ctx, term.paramType)
            if (!defEq(paramTypeType, Term.Sort))
                throw RuntimeException("Lambda parameter type is not a type")

            val bodyType = typeOf(ctx + term.paramType, term.body)

            Term.Pi(term.paramType, bodyType)
        }

        is Term.App -> {
            val fnType = normalize(typeOf(ctx, term.fn))

            if (fnType !is Term.Pi)
                throw RuntimeException("Applying non-function")

            val argType = typeOf(ctx, term.arg)

            if (!defEq(argType, fnType.paramType))
                throw RuntimeException("Argument type mismatch: " + argType + " != " + fnType.paramType)

            subst(fnType.body, 0, shift(term.arg, 1))
        }
    }
