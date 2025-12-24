package pleroma.aether

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AetherTest {
    @Test fun contextVar() {
        val ctx = listOf<Term>(Term.Sort(3), Term.Sort(0))

        assertEquals(
            Term.Sort(3),
            typeOf(mapOf(), ctx, Term.Var(1)),
        )

        assertEquals(
            Term.Sort(0),
            typeOf(mapOf(), ctx, Term.Var(0)),
        )
    }

    @Test fun highOrderAbs() {
        val env = mapOf("number" to EnvEntry(Term.Sort(0)))
        val ctx = listOf<Term>()

        val highOrderId = Term.Abs(Term.Pi(Term.Sort(0), Term.Sort(0)), Term.Var(0))

        assertEquals(
            Term.Pi(
                Term.Pi(Term.Sort(0), Term.Sort(0)),
                Term.Pi(Term.Sort(0), Term.Sort(0)),
            ),
            typeOf(env, ctx, highOrderId),
        )

        assertEquals(
            Term.Pi(Term.Sort(0), Term.Sort(0)),
            typeOf(env, ctx, Term.App(highOrderId, Term.Abs(Term.Sort(0), Term.Var(0)))),
        )

        assertEquals(
            Term.Sort(0),
            typeOf(
                env,
                ctx,
                Term.App(
                    Term.App(highOrderId, Term.Abs(Term.Sort(0), Term.Var(0))),
                    Term.Const("number"),
                ),
            ),
        )
    }

    @Test fun polymorphicApply() {
        val env =
            mapOf(
                "number" to EnvEntry(Term.Sort(0)),
                "string" to EnvEntry(Term.Sort(0)),
            )
        val ctx = listOf<Term>()

        val polyIdentity = Term.Abs(Term.Sort(0), Term.Abs(Term.Var(0), Term.Var(0)))
        val numberIdentity = Term.App(Term.App(polyIdentity, Term.Const("number")), Term.Num(42))
        val stringIdentity = Term.App(Term.App(polyIdentity, Term.Const("string")), Term.Str("foo"))

        assertEquals(Term.Const("number"), typeOf(env, ctx, numberIdentity))
        assertEquals(Term.Const("string"), typeOf(env, ctx, stringIdentity))
    }

    @Test fun kCombinator() {
        val env = mapOf("number" to EnvEntry(Term.Sort(0)), "string" to EnvEntry(Term.Sort(0)))
        val ctx = listOf<Term>()

        // A:T. B:T. x:A. y:B. x
        val kCombinator =
            Term.Abs(
                Term.Sort(0),
                Term.Abs(
                    Term.Sort(0),
                    Term.Abs(
                        Term.Var(1),
                        Term.Abs(
                            Term.Var(1),
                            Term.Var(1),
                        ),
                    ),
                ),
            )

        // K number string 42 "foo"
        assertEquals(
            Term.Const("number"),
            typeOf(
                env,
                ctx,
                Term.App(
                    Term.App(
                        Term.App(
                            Term.App(
                                kCombinator,
                                Term.Const("number"),
                            ),
                            Term.Const("string"),
                        ),
                        Term.Num(42),
                    ),
                    Term.Str("foo"),
                ),
            ),
        )

        // K string number "foo" 42
        assertEquals(
            Term.Const("string"),
            typeOf(
                env,
                ctx,
                Term.App(
                    Term.App(
                        Term.App(
                            Term.App(
                                kCombinator,
                                Term.Const("string"),
                            ),
                            Term.Const("number"),
                        ),
                        Term.Str("foo"),
                    ),
                    Term.Num(42),
                ),
            ),
        )
    }

    @Test fun absToPi() {
        // x:A. x
        assertEquals(
            Term.Pi(Term.Var(0), Term.Var(1)),
            typeOf(mapOf(), listOf(Term.Sort(0)), Term.Abs(Term.Var(0), Term.Var(0))),
        )

        // A:T. x:A. x
        assertEquals(
            Term.Pi(Term.Sort(0), Term.Pi(Term.Var(0), Term.Var(1))),
            typeOf(mapOf(), listOf(), Term.Abs(Term.Sort(0), Term.Abs(Term.Var(0), Term.Var(0)))),
        )

        // A:T'. B:T. x:B. x
        assertEquals(
            Term.Pi(Term.Sort(1), Term.Pi(Term.Sort(0), Term.Pi(Term.Var(0), Term.Var(1)))),
            typeOf(mapOf(), listOf(), Term.Abs(Term.Sort(1), Term.Abs(Term.Sort(0), Term.Abs(Term.Var(0), Term.Var(0))))),
        )
    }
}
