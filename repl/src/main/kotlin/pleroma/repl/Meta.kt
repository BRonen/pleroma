package pleroma.repl

import pleroma.aether.*

enum class Level { OBJECT, META }

data class Definition(val name: String, val level: Level, val term: Term)

class Resolver(
    val definitions: Map<String, Definition> = mapOf(),
    val bindings: List<String> = listOf(),
) {
    fun withBind(name: String) = Resolver(definitions, bindings + name)

    fun withDef(
        name: String,
        v: Definition,
    ) = Resolver(definitions + mapOf(name to v))
}

fun parseSequence(
    elements: List<Expr>,
    res: Resolver,
): Term {
    val head = elements.first()

    if (head == null) throw RuntimeException("Empty sequence")
    if (head !is Expr.Sym) throw RuntimeException("Expected symbol: " + head)

    return when (head.value) {
        "fn" -> {
            val params = elements[1]

            if (params !is Expr.Dict) throw RuntimeException("Expected Map: " + params)

            val body = parse(elements[2], res)

            params.elements.entries.fold(body) { acc, (n, t) ->
                Term.Abs(parse(t, res), acc)
            }
        }
        "quote" -> Term.Quote(parse(elements[1], res))
        "splice" -> Term.Splice(parse(elements[1], res))
        else -> {
            val definition = res.definitions[head.value]

            if (definition == null) throw RuntimeException("Definition not found: " + head)

            elements.drop(1).fold(parse(head, res)) { acc, arg ->
                if (definition.level == Level.META) {
                    Term.Exp(acc, parse(arg, res))
                } else {
                    Term.App(acc, parse(arg, res))
                }
            }
        }
    }
}

fun parse(
    expr: Expr,
    res: Resolver,
): Term =
    when (expr) {
        is Expr.Seq -> parseSequence(expr.elements, res)
        is Expr.Num -> Term.Num(expr.value)
        is Expr.Str -> Term.Str(expr.value)
        is Expr.Sym -> {
            val localIdx = res.locals.lastIndexOf(expr.value)
            if (localIdx != -1) {
                Term.Var(res.locals.size - 1 - localIdx)
            } else {
                val definition = res.definitions[expr.value]

                if (definition == null) throw RuntimeException("Unknown: ${expr.value}")

                Term.Const(expr.value)
            }
        }
        else -> throw RuntimeException("Unsupported")
    }

fun expandMacros(
    term: Term,
    env: Env,
): Term =
    when (term) {
        is Term.Exp -> {
            val definition = env[term.name]

            if (definition == null) throw RuntimeException("Macro not found: " + term)

            val applied = term.args.fold(definition.value) { acc, arg -> Term.App(acc, arg) }

            val result = normalize(env, applied)

            if (result is Term.Quote) result.expr else result
        }
        is Term.Pi -> Term.Pi(expandMacros(env, term.t), expandMacros(env, term.body))
        is Term.Abs -> Term.Abs(expandMacros(env, term.t), expandMacros(env, term.body))
        is Term.App -> Term.App(expandMacros(env, term.f), expandMacros(env, term.arg))
        else -> term
    }

fun parseModule(forms: List<Expr>) {
    val res = Resolver()
    val env = mapOf<String, EnvEntry>()

    for (form in forms) {
        if (form !is Expr.Seq) continue

        val head = form.elements[0]

        if (head !is Expr.Sym) throw RuntimeException("Trying to call a non-symbol at " + head)

        when (head.value) {
            "defmeta" -> {
                val name = form.elements[1]

                if (name !is Expr.Sym) throw RuntimeException("Trying to name a non-symbol at " + name)

                val body = parse(form.elements.last(), res)

                val type = typeOf(env, listOf(), body)

                res.definitions[name.value] = Definition(name.value, DefType.META, body)
                env[name.value] = EnvEntry(type, body)
            }
            "defn" -> {
                val name = form.elements[1]

                if (name !is Expr.Sym) throw RuntimeException("Trying to name a non-symbol at " + name)

                var body = parse(form.elements.last(), res)

                body = expandMacros(body, env)

                val type = typeOf(env, listOf(), body)

                res.definitions[name.value] = Definition(name.value, DefType.OBJECT, body)
                env[name.value] = EnvEntry(type, body)
            }
        }
    }
}
