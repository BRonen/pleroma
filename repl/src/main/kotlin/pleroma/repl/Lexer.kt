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
