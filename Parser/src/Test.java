import java.util.List;

public class Test {

    public static void main(String[] args) {
        // 1) precedence: 1 + (2*3)
        assertParseEquals(
                "x=1+2*3",
                List.of(new StrategyParser.Stmt.Assign(
                        "x",
                        new StrategyParser.Expr.Binary("+",
                                new StrategyParser.Expr.Num(1),
                                new StrategyParser.Expr.Binary("*",
                                        new StrategyParser.Expr.Num(2),
                                        new StrategyParser.Expr.Num(3)
                                )
                        )
                )),
                "precedence: + lower than *"
        );

        // 2) parentheses: (1+2) * 3  (ต้องมี Paren node)
        assertParseEquals(
                "x=(1+2)*3",
                List.of(new StrategyParser.Stmt.Assign(
                        "x",
                        new StrategyParser.Expr.Binary("*",
                                new StrategyParser.Expr.Paren(
                                        new StrategyParser.Expr.Binary("+",
                                                new StrategyParser.Expr.Num(1),
                                                new StrategyParser.Expr.Num(2)
                                        )
                                ),
                                new StrategyParser.Expr.Num(3)
                        )
                )),
                "parentheses become Expr.Paren"
        );

        // 3) right-assoc power: 2^(3^2)
        assertParseEquals(
                "x=2^3^2",
                List.of(new StrategyParser.Stmt.Assign(
                        "x",
                        new StrategyParser.Expr.Binary("^",
                                new StrategyParser.Expr.Num(2),
                                new StrategyParser.Expr.Binary("^",
                                        new StrategyParser.Expr.Num(3),
                                        new StrategyParser.Expr.Num(2)
                                )
                        )
                )),
                "right-associative ^"
        );

        // 4) info expr: nearby upleft
        assertParseEquals(
                "x=nearby upleft",
                List.of(new StrategyParser.Stmt.Assign(
                        "x",
                        new StrategyParser.Expr.Info(
                                StrategyParser.InfoKind.NEARBY,
                                StrategyParser.Direction.UPLEFT
                        )
                )),
                "info expr: nearby Direction"
        );

        // 5) info expr: ally/opponent (dirOrNull ต้องเป็น null)
        assertParseEquals(
                "a=ally b=opponent",
                List.of(
                        new StrategyParser.Stmt.Assign("a",
                                new StrategyParser.Expr.Info(StrategyParser.InfoKind.ALLY, null)),
                        new StrategyParser.Stmt.Assign("b",
                                new StrategyParser.Expr.Info(StrategyParser.InfoKind.OPPONENT, null))
                ),
                "info expr: ally/opponent"
        );

        // 6) statement parsing: move / shoot / done
        assertParseEquals(
                "move up shoot down 5 done",
                List.of(
                        new StrategyParser.Stmt.Move(StrategyParser.Direction.UP),
                        new StrategyParser.Stmt.Shoot(StrategyParser.Direction.DOWN, new StrategyParser.Expr.Num(5)),
                        new StrategyParser.Stmt.Done()
                ),
                "commands: move/shoot/done"
        );

        // 7) complex nesting: { x=1 while(1) { if(1) then done else move up } }
        assertParseEquals(
                "{ x=1 while(1) { if(1) then done else move up } }",
                List.of(
                        new StrategyParser.Stmt.Block(List.of(
                                new StrategyParser.Stmt.Assign("x", new StrategyParser.Expr.Num(1)),
                                new StrategyParser.Stmt.While(
                                        new StrategyParser.Expr.Num(1),
                                        new StrategyParser.Stmt.Block(List.of(
                                                new StrategyParser.Stmt.If(
                                                        new StrategyParser.Expr.Num(1),
                                                        new StrategyParser.Stmt.Done(),
                                                        new StrategyParser.Stmt.Move(StrategyParser.Direction.UP)
                                                )
                                        ))
                                )
                        ))
                ),
                "block/while/if nesting"
        );

        // 8) negative: nearby missing direction -> must throw ParseException
        assertParseThrows("x=nearby", "nearby must have direction");

        // 9) negative: missing ')'
        assertParseThrows("if(1 then done else done", "missing ')'");

        // 10) negative: missing '}'
        assertParseThrows("{ x=1 ", "missing '}'");

        System.out.println("\n✅ All AST tests passed!");
    }

    private static void assertParseEquals(String src, List<StrategyParser.Stmt> expected, String name) {
        try {
            var got = StrategyParser.parse(src);
            if (!got.equals(expected)) {
                System.out.println("\n❌ " + name);
                System.out.println("SRC: " + src);
                System.out.println("EXPECTED: " + expected);
                System.out.println("GOT     : " + got);
                throw new AssertionError("AST mismatch: " + name);
            }
            System.out.println("✓ " + name);
        } catch (Exception e) {
            System.out.println("\n❌ " + name + " threw exception");
            System.out.println("SRC: " + src);
            throw e;
        }
    }

    private static void assertParseThrows(String src, String name) {
        try {
            StrategyParser.parse(src);
            System.out.println("\n❌ " + name);
            System.out.println("SRC: " + src);
            throw new AssertionError("Expected ParseException but parsing succeeded: " + name);
        } catch (StrategyParser.ParseException e) {
            System.out.println("✓ " + name + " (threw ParseException)");
        }
    }
}
