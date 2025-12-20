package pleroma.repl

data class Location(
    val line: Int,
    val column: Int,
    val size: Int,
)

sealed class Token {
    data class LParen(val loc: Location) : Token()

    data class RParen(val loc: Location) : Token()

    data class LCurly(val loc: Location) : Token()

    data class RCurly(val loc: Location) : Token()

    data class LSquare(val loc: Location) : Token()

    data class RSquare(val loc: Location) : Token()

    data class SQuote(val loc: Location) : Token()

    data class Num(val value: Int, val loc: Location) : Token()

    data class Sym(val value: String, val loc: Location) : Token()

    data class Str(val value: String, val loc: Location) : Token()

    override fun toString(): String =
        when (this) {
            is LParen -> "("
            is RParen -> ")"
            is LCurly -> "{"
            is RCurly -> "}"
            is LSquare -> "["
            is RSquare -> "]"
            is SQuote -> "\'"
            is Num -> value.toString()
            is Sym -> value
            is Str -> "\"$value\""
        }
}

fun isWhitespace(ch: Char): Boolean = ch == ' ' || ch == '\n' || ch == '\t' || ch == ','

data class LexerState(
    val acc: StringBuilder = StringBuilder(),
    val tokens: MutableList<Token> = mutableListOf(),
    val line: Int = 1,
    val column: Int = 0,
) {
    fun copyState(
        acc: StringBuilder = this.acc,
        tokens: MutableList<Token> = this.tokens,
        line: Int = this.line,
        column: Int = this.column,
    ) = LexerState(acc, tokens, line, column)
}

fun makeLocation(
    state: LexerState,
    size: Int = 1,
): Location =
    Location(
        line = state.line,
        column = state.column - size,
        size = size,
    )

fun flushIdentifier(state: LexerState): LexerState {
    if (state.acc.isEmpty()) return state

    val text = state.acc.toString()
    val loc = makeLocation(state, text.length)

    state.tokens +=
        if (text.all { it.isDigit() }) {
            Token.Num(text.toInt(), loc)
        } else {
            Token.Sym(text, loc)
        }

    state.acc.clear()
    return state
}

fun updateLocation(
    state: LexerState,
    c: Char,
): LexerState =
    if (c == '\n') {
        state.copyState(line = state.line + 1, column = 0)
    } else {
        state.copyState(column = state.column + 1)
    }

fun tokenize(
    state: LexerState,
    ch: Char,
): LexerState {
    fun emitSimple(
        state: LexerState,
        tokenFactory: (Location) -> Token,
    ): LexerState {
        val st = flushIdentifier(state)
        val loc = makeLocation(st)
        st.tokens += tokenFactory(loc)
        return updateLocation(st, ch)
    }

    return when {
        isWhitespace(ch) -> updateLocation(flushIdentifier(state), ch)

        ch == '(' -> emitSimple(state) { Token.LParen(it) }
        ch == ')' -> emitSimple(state) { Token.RParen(it) }

        ch == '{' -> emitSimple(state) { Token.LCurly(it) }
        ch == '}' -> emitSimple(state) { Token.RCurly(it) }

        ch == '[' -> emitSimple(state) { Token.LSquare(it) }
        ch == ']' -> emitSimple(state) { Token.RSquare(it) }

        ch == '\'' -> emitSimple(state) { Token.SQuote(it) }

        else -> {
            val st = updateLocation(state, ch)
            st.acc.append(ch)
            st
        }
    }
}

fun stringToken(
    state: LexerState,
    inputChar: CharIterator,
): LexerState {
    var st = state
    val sb = StringBuilder()

    while (inputChar.hasNext()) {
        val c = inputChar.nextChar()

        when (c) {
            '"' -> {
                val loc =
                    Location(
                        line = state.line,
                        column = state.column,
                        size = sb.length + 2,
                    )
                st.tokens += Token.Str(sb.toString(), loc)
                return updateLocation(state, '"')
            }

            '\\' -> {
                if (!inputChar.hasNext()) {
                    throw RuntimeException("unterminated escape sequence")
                }

                val escaped = inputChar.nextChar()
                sb.append(
                    when (escaped) {
                        'n' -> '\n'
                        't' -> '\t'
                        'r' -> '\r'
                        '"' -> '"'
                        '\\' -> '\\'
                        else -> escaped
                    },
                )
                st = updateLocation(st, '\\')
                st = updateLocation(st, escaped)
            }

            else -> {
                sb.append(c)
                st = updateLocation(st, c)
            }
        }
    }

    throw RuntimeException("unterminated string literal at $state")
}

fun lexer(input: String): List<Token> {
    val iter = input.iterator()
    var state = LexerState()

    while (iter.hasNext()) {
        val c = iter.nextChar()

        if (c == '"') {
            state = flushIdentifier(state)
            state = updateLocation(state, '"')
            state = stringToken(state, iter)
        } else {
            state = tokenize(state, c)
        }
    }
    state = flushIdentifier(state)

    return state.tokens
}

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
    data class Def(val name: String, val params: List<String>, val body: Meta) : Expr()

    data class App(val callee: Meta, val args: List<Meta>) : Expr()

    data class Quote(val body: Code) : Expr()

    data class Parse(val body: Syntax) : Expr()

    data class Syntax(val value: Expr) : Expr()

    data class Code(val value: Obj, val type: String) : Expr()
}

data class MetaEnv(
    val metaDefs: Set<String> = emptySet(),
)

private fun parseDefMeta(
    args: List<Expr>,
    env: MetaEnv,
): Meta {
    require(args.size >= 3)

    val name = (args[0] as Expr.Sym).value

    val params =
        (args[1] as Expr.Seq).elements.map {
            (it as Expr.Sym).value
        }

    val bodyExpr = args[2]

    val extendedEnv =
        env.copy(
            metaDefs = env.metaDefs + name,
        )

    return Meta.Def(
        name = name,
        params = params,
        body = parseMeta(bodyExpr, extendedEnv),
    )
}

private fun parseMetaSeq(
    seq: Expr.Seq,
    env: MetaEnv,
): Meta {
    val elements = seq.elements
    require(elements.isNotEmpty())

    val head = elements.first()
    val args = elements.drop(1)

    val headSym =
        (head as? Expr.Sym)?.value
            ?: throw RuntimeException("invalid meta form")

    return when (headSym) {
        "defmeta" ->
            parseDefMeta(args, env)

        "quote" -> {
            require(args.size == 1)
            Meta.Quote(parseObj(args[0]))
        }

        "parse" -> {
            require(args.size == 1)
            Meta.Parse(Meta.Syntax(args[0]))
        }

        else ->
            if (headSym in env.metaDefs) {
                parseMetaCall(headSym, args)
            } else {
                throw RuntimeException("unknown meta function: $headSym")
            }
    }
}

fun parseMeta(
    expr: Expr,
    env: MetaEnv = MetaEnv(),
): Meta =
    when (expr) {
        is Expr.Quoted -> Meta.Syntax(expr.value)
        is Expr.Seq -> parseMetaSeq(expr, env)
        else -> throw RuntimeException("invalid meta expression: $expr")
    }

fun main() {
    val stdin = generateSequence(::readLine).joinToString("\n")
    val tokens = lexer(stdin)
    val expr = cparser(tokens)

    println(expr)
}
