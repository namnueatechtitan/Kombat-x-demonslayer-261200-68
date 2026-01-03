import java.util.*;

public final class StrategyParser {

    public enum TokenType {
        // Symbols
        PLUS, MINUS, STAR, SLASH, PERCENT,
        CARET, EQUAL, LPAREN, RPAREN, LBRACE, RBRACE,

        // Literals and identifiers
        NUMBER, IDENT,

        // Keywords
        DONE, MOVE, SHOOT, IF, THEN, ELSE, WHILE,
        ALLY, OPPONENT, NEARBY,

        // Directions
        UP, DOWN, UPLEFT, UPRIGHT, DOWNLEFT, DOWNRIGHT,

        // End of file
        EOF
    }

    public record Token(TokenType type, String lexeme, long longValue, int pos) {
        public Token(TokenType type, String lexeme, int pos) {
            this(type, lexeme, 0L, pos);
        }
    }

    public static final class Lexer {

        private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
                Map.entry("done", TokenType.DONE),
                Map.entry("move", TokenType.MOVE),
                Map.entry("shoot", TokenType.SHOOT),
                Map.entry("if", TokenType.IF),
                Map.entry("then", TokenType.THEN),
                Map.entry("else", TokenType.ELSE),
                Map.entry("while", TokenType.WHILE),
                Map.entry("ally", TokenType.ALLY),
                Map.entry("opponent", TokenType.OPPONENT),
                Map.entry("nearby", TokenType.NEARBY),
                Map.entry("up", TokenType.UP),
                Map.entry("down", TokenType.DOWN),
                Map.entry("upleft", TokenType.UPLEFT),
                Map.entry("upright", TokenType.UPRIGHT),
                Map.entry("downleft", TokenType.DOWNLEFT),
                Map.entry("downright", TokenType.DOWNRIGHT)
        );

        private static final Map<Character, TokenType> SYMBOLS = Map.ofEntries(
                Map.entry('+', TokenType.PLUS),
                Map.entry('-', TokenType.MINUS),
                Map.entry('*', TokenType.STAR),
                Map.entry('/', TokenType.SLASH),
                Map.entry('%', TokenType.PERCENT),
                Map.entry('^', TokenType.CARET),
                Map.entry('=', TokenType.EQUAL),
                Map.entry('(', TokenType.LPAREN),
                Map.entry(')', TokenType.RPAREN),
                Map.entry('{', TokenType.LBRACE),
                Map.entry('}', TokenType.RBRACE)
        );

        public static List<Token> lex(String src) {
            List<Token> tokens = new ArrayList<>();
            int pos = 0;

            while (pos < src.length()) {
                char c = src.charAt(pos);

                if (Character.isWhitespace(c)) {
                    pos++;
                    continue;
                }

                if (c == '#') { // comment to end of line
                    while (pos < src.length() && src.charAt(pos) != '\n') pos++;
                    continue;
                }

                if (Character.isDigit(c)) {
                    int start = pos;
                    while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
                    String s = src.substring(start, pos);
                    long v;
                    try {
                        v = Long.parseLong(s);
                    } catch (NumberFormatException e) {
                        throw new ParseException("Number out of range for long", s, start);
                    }
                    tokens.add(new Token(TokenType.NUMBER, s, v, start));
                    continue;
                }

                if (Character.isLetter(c)) {
                    int start = pos;
                    pos++;
                    while (pos < src.length() && Character.isLetterOrDigit(src.charAt(pos))) pos++;
                    String ident = src.substring(start, pos);

                    TokenType kw = KEYWORDS.get(ident);
                    if (kw != null) tokens.add(new Token(kw, ident, start));
                    else tokens.add(new Token(TokenType.IDENT, ident, start));
                    continue;
                }

                TokenType sym = SYMBOLS.get(c);
                if (sym != null) {
                    tokens.add(new Token(sym, String.valueOf(c), pos));
                    pos++;
                    continue;
                }

                throw new ParseException("Unexpected character: " + c, pos);
            }

            tokens.add(new Token(TokenType.EOF, "", pos));
            return tokens;
        }
    }

    public enum Direction { UP, DOWN, UPLEFT, UPRIGHT, DOWNLEFT, DOWNRIGHT }
    public enum InfoKind { ALLY, OPPONENT, NEARBY }

    public sealed interface Stmt permits
            Stmt.Assign, Stmt.Done, Stmt.Move, Stmt.Shoot,
            Stmt.Block, Stmt.If, Stmt.While {

        record Assign(String name, Expr value) implements Stmt {}
        record Done() implements Stmt {}
        record Move(Direction dir) implements Stmt {}
        record Shoot(Direction dir, Expr expenditure) implements Stmt {}
        record Block(List<Stmt> statements) implements Stmt {}
        record If(Expr cond, Stmt thenBranch, Stmt elseBranch) implements Stmt {}
        record While(Expr cond, Stmt body) implements Stmt {}
    }

    public sealed interface Expr permits
            Expr.Num, Expr.Var, Expr.Binary, Expr.Paren, Expr.Info {

        record Num(long value) implements Expr {}
        record Var(String name) implements Expr {}
        record Binary(String op, Expr left, Expr right) implements Expr {}
        record Paren(Expr inner) implements Expr {}
        record Info(InfoKind kind, Direction dirOrNull) implements Expr {}
    }

    public static final class ParseException extends RuntimeException {
        public ParseException(String message, int pos) {
            super("Error at position " + pos + ": " + message);
        }
        public ParseException(String message, String lexeme, int pos) {
            super("Error at position " + pos + " near '" + lexeme + "': " + message);
        }
    }

    public static final class Parser {
        private final List<Token> tokens;
        private int current = 0;

        public Parser(List<Token> tokens) {
            this.tokens = tokens;
        }

        public List<Stmt> parseStrategy() {
            List<Stmt> statements = new ArrayList<>();
            while (!isAtEnd()) statements.add(parseStatement());
            if (statements.isEmpty()) throw error(peek(), "Expected at least one statement");
            return statements;
        }

        private Stmt parseStatement() {
            return switch (peek().type()) {
                case IF -> parseIfStatement();
                case WHILE -> parseWhileStatement();
                case LBRACE -> parseBlockStatement();
                case DONE -> { advance(); yield new Stmt.Done(); }
                case MOVE -> parseMoveStatement();
                case SHOOT -> parseShootStatement();
                case IDENT -> parseAssignmentStatement();
                default -> throw error(peek(), "Expected statement");
            };
        }

        private Stmt parseIfStatement() {
            consume(TokenType.IF, "Expected 'if'");
            consume(TokenType.LPAREN, "Expected '(' after 'if'");
            Expr cond = parseExpression();
            consume(TokenType.RPAREN, "Expected ')' after condition");
            consume(TokenType.THEN, "Expected 'then' after condition");
            Stmt thenBranch = parseStatement();
            consume(TokenType.ELSE, "Expected 'else' after then branch");
            Stmt elseBranch = parseStatement();
            return new Stmt.If(cond, thenBranch, elseBranch);
        }

        private Stmt parseWhileStatement() {
            consume(TokenType.WHILE, "Expected 'while'");
            consume(TokenType.LPAREN, "Expected '(' after 'while'");
            Expr cond = parseExpression();
            consume(TokenType.RPAREN, "Expected ')' after condition");
            Stmt body = parseStatement();
            return new Stmt.While(cond, body);
        }

        private Stmt parseBlockStatement() {
            consume(TokenType.LBRACE, "Expected '{'");
            List<Stmt> statements = new ArrayList<>();
            while (!check(TokenType.RBRACE) && !isAtEnd()) statements.add(parseStatement());
            consume(TokenType.RBRACE, "Expected '}' after block");
            return new Stmt.Block(statements);
        }

        private Stmt parseMoveStatement() {
            consume(TokenType.MOVE, "Expected 'move'");
            Direction dir = parseDirection();
            return new Stmt.Move(dir);
        }

        private Stmt parseShootStatement() {
            consume(TokenType.SHOOT, "Expected 'shoot'");
            Direction dir = parseDirection();
            Expr expenditure = parseExpression();
            return new Stmt.Shoot(dir, expenditure);
        }

        private Stmt parseAssignmentStatement() {
            Token name = consume(TokenType.IDENT, "Expected identifier");
            consume(TokenType.EQUAL, "Expected '=' after identifier");
            Expr value = parseExpression();
            return new Stmt.Assign(name.lexeme(), value);
        }

        private Direction parseDirection() {
            Token token = advance();
            return switch (token.type()) {
                case UP -> Direction.UP;
                case DOWN -> Direction.DOWN;
                case UPLEFT -> Direction.UPLEFT;
                case UPRIGHT -> Direction.UPRIGHT;
                case DOWNLEFT -> Direction.DOWNLEFT;
                case DOWNRIGHT -> Direction.DOWNRIGHT;
                default -> throw error(token, "Expected direction");
            };
        }

        private Expr parseExpression() { return parseAddSub(); }

        private Expr parseAddSub() {
            Expr expr = parseMulDivMod();
            while (match(TokenType.PLUS, TokenType.MINUS)) {
                Token op = previous();
                Expr right = parseMulDivMod();
                expr = new Expr.Binary(op.lexeme(), expr, right);
            }
            return expr;
        }

        private Expr parseMulDivMod() {
            Expr expr = parsePower();
            while (match(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
                Token op = previous();
                Expr right = parsePower();
                expr = new Expr.Binary(op.lexeme(), expr, right);
            }
            return expr;
        }

        private Expr parsePower() {
            Expr left = parsePrimary();
            if (match(TokenType.CARET)) {
                Token op = previous();
                Expr right = parsePower(); // right-assoc
                return new Expr.Binary(op.lexeme(), left, right);
            }
            return left;
        }

        private Expr parsePrimary() {
            return switch (peek().type()) {
                case NUMBER -> new Expr.Num(advance().longValue());
                case IDENT -> new Expr.Var(advance().lexeme());
                case LPAREN -> parseParenExpression();
                case ALLY -> parseInfo(InfoKind.ALLY);
                case OPPONENT -> parseInfo(InfoKind.OPPONENT);
                case NEARBY -> parseNearby();
                default -> throw error(peek(), "Expected expression");
            };
        }

        private Expr parseParenExpression() {
            consume(TokenType.LPAREN, "Expected '('");
            Expr expr = parseExpression();
            consume(TokenType.RPAREN, "Expected ')'");
            return new Expr.Paren(expr);
        }

        private Expr parseInfo(InfoKind kind) {
            advance();
            return new Expr.Info(kind, null);
        }

        private Expr parseNearby() {
            consume(TokenType.NEARBY, "Expected 'nearby'");
            Direction dir = parseDirection();
            return new Expr.Info(InfoKind.NEARBY, dir);
        }

        private boolean match(TokenType... types) {
            for (TokenType type : types) {
                if (check(type)) { advance(); return true; }
            }
            return false;
        }

        private boolean check(TokenType type) {
            return peek().type() == type;
        }

        private Token consume(TokenType type, String message) {
            if (check(type)) return advance();
            throw error(peek(), message);
        }

        private Token advance() {
            if (!isAtEnd()) current++;
            return previous();
        }

        private boolean isAtEnd() {
            return peek().type() == TokenType.EOF;
        }

        private Token peek() { return tokens.get(current); }
        private Token previous() { return tokens.get(current - 1); }

        private ParseException error(Token token, String message) {
            return new ParseException(message, token.lexeme(), token.pos());
        }
    }

    public static List<Stmt> parse(String src) {
        return new Parser(Lexer.lex(src)).parseStrategy();
    }
}
