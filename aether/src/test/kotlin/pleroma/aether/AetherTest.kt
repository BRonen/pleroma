package pleroma.aether

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AetherTest {
    @Test fun contextVar() {
        val env =
            mapOf(
                "number" to Term.Sort(0),
                "string" to Term.Sort(0),
            )
        val ctx = listOf<Term>(Term.Sort(3), Term.Sort(0))

        val input = Term.Var(1)
        val expected = Term.Sort(3)

        val actual = typeOf(env, ctx, input)

        assertEquals(expected, actual)
    }

    @Test fun pi() {
        val env =
            mapOf(
                "number" to Term.Sort(0),
                "string" to Term.Sort(0),
            )
        val ctx = listOf<Term>(Term.Sort(0))

        val input = Term.Abs(Term.Var(0), Term.Var(0))
        val expected = Term.Pi(Term.Var(0), Term.Var(0))
        val actual = typeOf(env, ctx, input)

        assertEquals(expected, actual)
    }

    @Test fun pi2() {
        val env =
            mapOf(
                "number" to Term.Sort(0),
                "string" to Term.Sort(0),
            )
        val ctx = listOf<Term>(Term.Sort(0))

        val input = Term.Abs(Term.App(Term.Abs(Term.Sort(0), Term.Var(0)), Term.Var(0)), Term.Var(0))
        val expected =
            Term.Pi(
                Term.App(Term.Abs(Term.Sort(0), Term.Var(0)), Term.Var(0)),
                Term.App(Term.Abs(Term.Sort(0), Term.Var(0)), Term.Var(0)),
            )
        val actual = typeOf(env, ctx, input)

        assertEquals(expected, actual)
    }

    // @Test fun pi3() {
    //     val env =
    //         mapOf(
    //             "number" to Term.Sort(0),
    //             "string" to Term.Sort(0),
    //         )
    //     val ctx = listOf<Term>(Term.Sort(0))

    //     val input =
    //         Term.App(
    //             Term.Abs(Term.App(Term.Abs(Term.Sort(0), Term.Var(0)), Term.Var(0)), Term.Var(0)),
    //             Term.Var(0),
    //         )
    //     val expected = Term.App(Term.Abs(Term.Sort(0), Term.Var(0)), Term.Var(0))
    //     val actual = typeOf(env, ctx, input)

    //     assertEquals(expected, actual)
    // }

    @Test fun secondOrderAbstraction() {
        val env =
            mapOf(
                "number" to Term.Sort(0),
                "string" to Term.Sort(0),
            )
        val ctx = listOf<Term>()

        val input =
            Term.Abs(
                Term.Pi(
                    Term.Sort(0),
                    Term.Sort(0),
                ),
                Term.Var(0),
            )
        val expected =
            Term.Pi(
                Term.Pi(
                    Term.Sort(0),
                    Term.Sort(0),
                ),
                Term.Pi(
                    Term.Sort(0),
                    Term.Sort(0),
                ),
            )

        val actual = typeOf(env, ctx, input)

        assertEquals(expected, actual)
    }

    @Test fun abstractionNumberApply() {
        val env =
            mapOf(
                "number" to Term.Sort(0),
                "string" to Term.Sort(0),
            )
        val ctx = listOf<Term>()

        val input =
            Term.App(
                Term.Abs(
                    Term.Const("number"),
                    Term.Var(0),
                ),
                Term.Num(42),
            )
        val expected = Term.Const("number")
        val actual = typeOf(env, ctx, input)

        assertEquals(expected, actual)
    }

    @Test fun abstractionTypeApply() {
        val env =
            mapOf(
                "number" to Term.Sort(0),
                "string" to Term.Sort(0),
            )
        val ctx = listOf<Term>()

        val input =
            Term.App(
                Term.Abs(
                    Term.Sort(0),
                    Term.Var(0),
                ),
                Term.Const("number"),
            )
        val expected = Term.Sort(0)
        val actual = typeOf(env, ctx, input)

        assertEquals(expected, actual)
    }

    @Test fun genericNumberApply() {
        val env =
            mapOf(
                "number" to Term.Sort(0),
                "string" to Term.Sort(0),
            )
        val ctx = listOf<Term>()

        val input =
            Term.App(
                Term.App(
                    Term.Abs(
                        Term.Sort(0),
                        Term.Abs(
                            Term.Var(0),
                            Term.Var(1),
                        ),
                    ),
                    Term.Const("number"),
                ),
                Term.Num(42),
            )
        val expected = Term.Const("number")
        val actual = typeOf(env, ctx, input)

        assertEquals(expected, actual)
    }

    // @Test fun genericStringApply() {
    //     val env =
    //         mapOf(
    //             "number" to Term.Sort(0),
    //             "string" to Term.Sort(0),
    //         )
    //     val ctx = listOf<Term>()

    //     val input =
    //         Term.App(
    //             Term.Abs(
    //                 Term.App(
    //                     Term.Abs(
    //                         Term.Sort(0),
    //                         Term.Var(0),
    //                     ),
    //                     Term.Const("string"),
    //                 ),
    //                 Term.Var(0),
    //             ),
    //             Term.Str("42"),
    //         )
    //     val expected = Term.Const("string")
    //     val actual = typeOf(env, ctx, input)

    //     assertEquals(expected, actual)
    // }

    @Test fun nestedPi() {
        val env =
            mapOf(
                "number" to Term.Sort(0),
                "string" to Term.Sort(0),
            )
        val ctx = listOf<Term>()

        val input =
            Term.Abs(
                Term.Sort(0),
                Term.Abs(
                    Term.Var(0),
                    Term.Var(0),
                ),
            )
        val expected =
            Term.Pi(
                Term.Sort(0),
                Term.Pi(
                    Term.Var(0),
                    Term.Var(0),
                ),
            )
        val actual = typeOf(env, ctx, input)

        assertEquals(expected, actual)
    }
}
