package pleroma.repl

import pleroma.aether.*

sealed class Expr {
    data class Num(val value: Int, val loc: Location) : Expr()

    data class Str(val value: String, val loc: Location) : Expr()

    data class Sym(val value: String, val loc: Location) : Expr()

    data class Quote(val expr: Expr) : Expr()

    data class Splice(val expr: Expr) : Expr()

    data class Dict(val elements: Map<String, Expr>, val loc: Location) : Expr()

    data class Seq(val elements: List<Expr>, val loc: Location) : Expr()

    data class Vec(val elements: List<Expr>, val loc: Location) : Expr()
}

fun parseSequence(
    tokens: List<Token>,
    closing: (Token) -> Boolean,
    makeExpr: (List<Expr>) -> Expr,
): Pair<Expr, List<Token>> {
    var rest = tokens
    val elements = mutableListOf<Expr>()

    while (rest.isNotEmpty() && !closing(rest.first())) {
        val (expr, next) = cparser(rest)
        elements += expr
        rest = next
    }

    if (rest.isEmpty()) {
        throw RuntimeException("unexpected EOF")
    }

    return makeExpr(elements) to rest.drop(1)
}

fun cparser(tokens: List<Token>): Pair<Expr, List<Token>> {
    val expr =
        when (val f = tokens.first()) {
            is Token.Str -> Expr.Str(f.value, f.loc) to tokens.drop(1)
            is Token.Sym -> Expr.Sym(f.value, f.loc) to tokens.drop(1)
            is Token.Num -> Expr.Num(f.value, f.loc) to tokens.drop(1)
            is Token.SQuote -> {
                val (expr, rst) = cparser(tokens.drop(1))
                Expr.Quote(expr) to rst
            }
            is Token.Tilde -> {
                val (expr, rst) = cparser(tokens.drop(1))
                Expr.Splice(expr) to rst
            }
            is Token.LParen ->
                parseSequence(
                    tokens.drop(1),
                    closing = { it is Token.RParen },
                    makeExpr = { Expr.Seq(it, f.loc) },
                )
            is Token.LSquare ->
                parseSequence(
                    tokens.drop(1),
                    closing = { it is Token.RSquare },
                    makeExpr = { Expr.Vec(it, f.loc) },
                )
            is Token.LCurly -> {
                var rest = tokens.drop(1)
                val elements = mutableMapOf<String, Expr>()
                var currentKey: String? = null

                while (rest.isNotEmpty() && rest.first() !is Token.RCurly) {
                    val (expr, next) = cparser(rest)

                    if (currentKey == null) {
                        if (expr is Expr.Sym) {
                            currentKey = expr.value
                        } else {
                            throw RuntimeException("Trying invalid expr as key on a map: $currentKey")
                        }
                    } else {
                        elements[currentKey] = expr
                        currentKey = null
                    }

                    rest = next
                }

                if (currentKey != null) throw RuntimeException("Invalid number of elements on map: $currentKey")

                if (rest.isEmpty()) throw RuntimeException("unexpected EOF: $f")

                return Expr.Dict(mapOf<String, Expr>() + elements, f.loc) to rest.drop(1)
            }
            else -> throw RuntimeException("Invalid token received")
        }

    return expr
}
