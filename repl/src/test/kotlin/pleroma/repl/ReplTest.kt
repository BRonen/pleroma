package pleroma.repl

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class ReplTest {
    @Test fun testGetMessage() {
        val code = "(print hello \"world\" (+ 1 2) [1 2 3] {:foo :bar})"
        val tokens = lexer(code)
        val tokensExpected = listOf<Token>(
            Token.LParen(Location(line=1, column=-1, size=1)),
            Token.Sym("print", Location(line=1, column=1, size=5)),
            Token.Sym("hello", Location(line=1, column=7, size=5)),
            Token.Str("world", Location(line=1, column=14, size=7)),
            Token.LParen(Location(line=1, column=15, size=1)),
            Token.Sym("+", Location(line=1, column=17, size=1)),
            Token.Num(1, Location(line=1, column=19, size=1)),
            Token.Num(2, Location(line=1, column=21, size=1)),
            Token.RParen(Location(line=1, column=21, size=1)),
            Token.LSquare(Location(line=1, column=23, size=1)),
            Token.Num(1, Location(line=1, column=25, size=1)),
            Token.Num(2, Location(line=1, column=27, size=1)),
            Token.Num(3, Location(line=1, column=29, size=1)),
            Token.RSquare(Location(line=1, column=29, size=1)),
            Token.LCurly(Location(line=1, column=31, size=1)),
            Token.Sym(":foo", Location(line=1, column=33, size=4)),
            Token.Sym(":bar", Location(line=1, column=38, size=4)),
            Token.RCurly(Location(line=1, column=41, size=1)),
            Token.RParen(Location(line=1, column=42, size=1))
        )

        assertEquals(tokensExpected, tokens)

        val expr = cparser(tokens)
        val exprExpected = Expr.Seq(
            listOf<Expr>(
                Expr.Sym("print", Location(line=1, column=1, size=5)),
                Expr.Sym("hello", Location(line=1, column=7, size=5)),
                Expr.Str("world", Location(line=1, column=14, size=7)),
                Expr.Seq(
                    listOf<Expr>(
                        Expr.Sym("+", Location(line=1, column=17, size=1)),
                        Expr.Num(1, Location(line=1, column=19, size=1)),
                        Expr.Num(2, Location(line=1, column=21, size=1))
                    )
                ),
                Expr.Vec(
                    listOf<Expr>(
                        Expr.Num(1, Location(line=1, column=25, size=1)),
                        Expr.Num(2, Location(line=1, column=27, size=1)),
                        Expr.Num(3, Location(line=1, column=29, size=1))
                    )
                ),
                Expr.Map(
                    listOf<Expr>(
                        Expr.Sym(":foo", Location(line=1, column=33, size=4)),
                        Expr.Sym(":bar", Location(line=1, column=38, size=4))
                    )
                )
            )
        ) to listOf<Token>()

        assertEquals(exprExpected, expr)
    }
}
