package pleroma.repl

sealed class Expr {
    data class Num(val value: Int, val loc: Location) : Expr()

    data class Str(val value: String, val loc: Location) : Expr()

    data class Sym(val value: String, val loc: Location) : Expr()

    data class Quoted(val value: Expr) : Expr()

    data class Map(val elements: List<Expr>) : Expr()

    data class Seq(val elements: List<Expr>) : Expr()

    data class Vec(val elements: List<Expr>) : Expr()
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
                Expr.Quoted(expr) to rst
            }
            is Token.LParen ->
                parseSequence(
                    tokens.drop(1),
                    closing = { it is Token.RParen },
                    makeExpr = { Expr.Seq(it) },
                )
            is Token.LCurly ->
                parseSequence(
                    tokens.drop(1),
                    closing = { it is Token.RCurly },
                    makeExpr = { Expr.Map(it) },
                )
            is Token.LSquare ->
                parseSequence(
                    tokens.drop(1),
                    closing = { it is Token.RSquare },
                    makeExpr = { Expr.Vec(it) },
                )
            else -> throw RuntimeException("Invalid token received")
        }

    return expr
}

sealed class Obj {
    data class Def(val name: String, val params: List<String>, val body: Meta) : Expr()

    data class App(val callee: Obj, val args: List<Obj>) : Expr()

    data class Num(val value: Int) : Expr()

    data class Sym(val value: String) : Expr()

    data class Str(val value: String) : Expr()

    data class Splice(val code: Meta.Code) : Obj()
}

sealed class Meta {
    data class Def(val name: String, val params: List<String>, val body: Meta) : Meta()

    data class App(val callee: Meta, val args: List<Meta>) : Meta()

    data class Quote(val body: Code) : Meta()

    data class Parse(val body: Syntax) : Meta()

    data class Syntax(val value: Expr) : Meta()

    data class Code(val value: Obj, val type: String) : Meta()
}

data class MetaEnv(
    val metaDefs: Set<String> = emptySet(),
)
