package pleroma.aether

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class AetherTest {
    @Test fun VarTest() {
        val ctx = listOf(Term.Sort, Term.Sort)

        // (VAR 1) : [SORT; Sort]
        val ty = typeOf(ctx, Term.Var(1))

        assertEquals(Term.Sort, ty)
    }

    @Test fun PiTest() {
        val ctx = listOf(Term.Sort)

        // (PI (VAR 0) (VAR 0)) : [Sort]
        val id = Term.Lam(Term.Var(0), Term.Var(0))

        val ty = typeOf(ctx, id)

        assertEquals(Term.Pi(Term.Var(0), Term.Var(0)), ty)
    }

    @Test fun nestedPiTest() {
        val ctx = listOf(Term.Sort, Term.Sort)

        // (PI (VAR 1) (PI (VAR 1) (VAR 1))) : [SORT; Sort]
        val ty = typeOf(
            ctx,
            Term.Lam(
                Term.Var(1),
                Term.Lam(
                    Term.Var(1),
                    Term.Var(1)
                )
            )
        )

        assertEquals(Term.Pi(Term.Var(1), Term.Pi(Term.Var(1), Term.Var(1))), ty)
    }
    
    @Test fun AppTest() {
        val ctx = listOf(Term.Sort, Term.Sort)

        // (APP (PI (VAR 1) (VAR 0)) (VAR 0)) : [SORT; (VAR 0)]
        val ty = typeOf(
            ctx,
            Term.App(
                Term.Lam(
                    Term.Var(1),
                    Term.Var(0)
                ),
                Term.Var(0)
            )
        )

        println(ty)

        assertEquals(Term.Var(271863), ty)
    }
}
