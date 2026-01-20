package pleroma.repl

enum class Level { OBJECT, META }

data class Definition(val name: String, val level: Level, val term: Term)

class Resolver(
    val definitions: Map<String, Definition> = mapOf(),
    val bindings: List<String> = listOf(),
) {
    fun withBind(name: String) = Resolver(definitions, bindings + name)

    fun withDef(
        name: String,
        value: Term,
    ) = Resolver(definitions + mapOf(name to value))
}

fun parseSequence(
    elements: List<Expr>,
    res: Resolver,
): Term {
    val head = elements.first()

    if (head !is Expr.Sym) throw RuntimeException("Expected symbol: " + head.loc)

    return when (head.value) {
        "fn" -> {
            val params = elements[1]

            if (params !is Expr.Map) throw RuntimeException("Expected Map: " + params.loc)

            var body = elements[2]

            for ((n, t) in params) {
                body = Term.Abs(t, parse(body, res.withLocal(n)))
            }

            body
        }
        "quote" -> Term.Quote(parse(elements[1], res))
        "splice" -> Term.Splice(parse(elements[1], res))
        else -> {
            val definition = res.definitions[head.value]

            if (definition == null) throw RuntimeException("Definition " + head.value + " not found at " + head.loc)

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

fun parseModule(forms: List<Expr>) {
    val res = Resolver()
    val env = mapOf<String, EnvEntry>()

    for (form in forms) {
        if (form !is Expr.Seq) continue

        val head = form.elements[0]

        if (head !is Expr.Sym) throw RuntimeException("Trying to call a non-symbol at " + head.loc)

        when (head) {
            "defmeta" -> {
            }
            "defn" -> {
            }
        }
    }
}
