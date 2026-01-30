package pleroma.repl

fun main() {
    val stdin = generateSequence(::readLine).joinToString("\n")
    val tokens = lexer(stdin)
    val expr = cparser(tokens)

    println(expr)
}
