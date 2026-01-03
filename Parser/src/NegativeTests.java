

import java.util.List;

public class NegativeTests {

    private static int passed = 0;
    private static int failed = 0;

    record Case(String src, String name) {}

    public static void main(String[] args) {
        System.out.println("=== StrategyParser Negative Test Suite ===");

        // เคสที่ "ควรพัง" (ต้อง throw StrategyParser.ParseException)
        Case[] shouldFail = new Case[] {
                // A) reserved/identifier
                new Case("if=1", "reserved word as identifier"),
                new Case("move=3", "keyword used as identifier"),
                new Case("nearby=2", "keyword used as identifier (nearby)"),

                // B) info expression errors
                new Case("x=nearby", "nearby missing direction"),
                new Case("x=nearby 3", "nearby wrong argument (number)"),
                new Case("x=nearby foo", "nearby wrong argument (identifier)"),

                // C) command missing parts
                new Case("move", "move missing direction"),
                new Case("shoot 5", "shoot missing direction"),
                new Case("shoot up", "shoot missing expression"),
                new Case("move upleftt", "unknown direction"),

                // D) parentheses/braces missing
                new Case("if 1) then done else done", "if missing '('"),
                new Case("if(1 then done else done", "if missing ')'"),
                new Case("if(1) done else done", "if missing 'then'"),
                new Case("if(1) then done", "if missing 'else'"),
                new Case("while(1 { done }", "while missing ')'"),
                new Case("{ x=1", "block missing '}'"),

                // E) expression errors (no unary in grammar)
                new Case("x=+1", "unary plus not allowed"),
                new Case("x=1+", "missing rhs operand"),
                new Case("x=2^", "power missing rhs"),
                new Case("x=(1+2", "expression missing ')'"),
                new Case("x=1**2", "double operator '**' invalid"),

                // F) unexpected characters / separators not in grammar
                new Case("x=@", "unexpected character"),
                new Case("x=1;", "semicolon not supported"),
        };

        for (Case c : shouldFail) {
            expectFail(c.src(), c.name());
        }

        // เคสที่ "ควรผ่าน" เพิ่มบางอันเพื่อกัน false positive
        Case[] shouldPass = new Case[] {
                new Case("then1=3", "identifier that starts with keyword prefix (then1) should pass"),
                new Case("if1=2", "identifier that starts with keyword prefix (if1) should pass"),
                new Case("x=2^3^2^1", "right-assoc power chain should pass"),
                new Case("{ x=1 while(1) { if(1) then done else move up } }", "nested block/while/if should pass"),
        };

        for (Case c : shouldPass) {
            expectPass(c.src(), c.name());
        }

        System.out.printf("%nSummary: %d passed, %d failed%n", passed, failed);
        if (failed > 0) {
            throw new AssertionError("Some negative tests failed. See output above.");
        }
        System.out.println("✅ All negative tests behaved as expected!");
    }

    private static void expectFail(String src, String name) {
        System.out.printf("EXPECT_FAIL: %-40s ... ", name);
        try {
            List<StrategyParser.Stmt> ast = StrategyParser.parse(src);
            // ถ้ามาถึงตรงนี้ แปลว่าไม่พัง ทั้งที่ควรพัง
            System.out.println("✗ SHOULD HAVE FAILED (parsed: " + ast + ")");
            failed++;
        } catch (StrategyParser.ParseException e) {
            System.out.println("✓");
            passed++;
        } catch (Exception e) {
            System.out.println("✗ WRONG EXCEPTION: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            failed++;
        }
    }

    private static void expectPass(String src, String name) {
        System.out.printf("PASS:        %-40s ... ", name);
        try {
            StrategyParser.parse(src);
            System.out.println("✓");
            passed++;
        } catch (Exception e) {
            System.out.println("✗ FAILED: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            failed++;
        }
    }
}
