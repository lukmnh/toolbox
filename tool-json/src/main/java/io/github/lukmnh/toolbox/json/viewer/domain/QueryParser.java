package io.github.lukmnh.toolbox.json.viewer.domain;

import io.github.lukmnh.toolbox.json.shared.domain.InvalidQueryException;
import io.github.lukmnh.toolbox.json.shared.domain.InvalidQueryException.Reason;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Parser recursive descent untuk filter query.
 *
 * <pre>
 * query      := or EOF
 * or         := and ("OR" and)*
 * and        := primary ("AND" primary)*
 * primary    := "(" or ")" | path ("exists" | "isNull" | operator value)
 * value      := "string" | number | true | false | null
 * </pre>
 *
 * Kata kunci tidak peka huruf besar/kecil. {@code AND} lebih kuat dari {@code OR}.
 */
public final class QueryParser {

    private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?");
    private static final String AND = "AND";
    private static final String OR = "OR";
    private static final String EXISTS = "exists";
    private static final String IS_NULL = "isNull";

    private final List<Token> tokens;
    private int cursor;

    private QueryParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /** @throws InvalidQueryException dengan posisi karakter yang bermasalah */
    public static Query parse(String text) {
        if (text == null || text.isBlank()) {
            throw new InvalidQueryException(Reason.EMPTY, 0, "");
        }
        QueryParser parser = new QueryParser(new Lexer(text).tokenize());
        Query query = parser.parseOr();
        Token rest = parser.peek();
        if (rest.type() != TokenType.END) {
            throw new InvalidQueryException(Reason.UNEXPECTED_TOKEN, rest.position(), rest.text());
        }
        return query;
    }

    private Query parseOr() {
        Query left = parseAnd();
        while (peek().isKeyword(OR)) {
            cursor++;
            left = new Query.Or(left, parseAnd());
        }
        return left;
    }

    private Query parseAnd() {
        Query left = parsePrimary();
        while (peek().isKeyword(AND)) {
            cursor++;
            left = new Query.And(left, parsePrimary());
        }
        return left;
    }

    private Query parsePrimary() {
        Token token = next();
        if (token.type() == TokenType.OPEN_PAREN) {
            Query inner = parseOr();
            if (next().type() != TokenType.CLOSE_PAREN) {
                throw new InvalidQueryException(Reason.UNCLOSED_PARENTHESIS, token.position(), token.text());
            }
            return inner;
        }
        if (token.type() != TokenType.WORD || token.isKeyword(AND) || token.isKeyword(OR)) {
            throw new InvalidQueryException(Reason.EXPECTED_FIELD, token.position(), token.text());
        }
        return parseCondition(FieldPath.parse(token.text(), token.position()));
    }

    private Query parseCondition(FieldPath path) {
        Token token = next();
        if (token.isKeyword(EXISTS)) {
            return new Query.Exists(path);
        }
        if (token.isKeyword(IS_NULL)) {
            return new Query.IsNull(path);
        }
        Optional<QueryOperator> operator =
                token.type() == TokenType.STRING ? Optional.empty() : QueryOperator.fromSymbol(token.text());
        if (operator.isEmpty()) {
            throw new InvalidQueryException(Reason.EXPECTED_OPERATOR, token.position(), token.text());
        }
        return new Query.Comparison(path, operator.get(), parseValue());
    }

    private JsonValue parseValue() {
        Token token = next();
        if (token.type() == TokenType.STRING) {
            return new JsonValue.JsonString(token.text());
        }
        if (token.type() == TokenType.WORD) {
            Optional<JsonValue> literal = literal(token.text());
            if (literal.isPresent()) {
                return literal.get();
            }
        }
        throw new InvalidQueryException(Reason.EXPECTED_VALUE, token.position(), token.text());
    }

    private static Optional<JsonValue> literal(String word) {
        if (NUMBER.matcher(word).matches()) {
            return Optional.of(new JsonValue.JsonNumber(word));
        }
        return switch (word) {
            case "true" -> Optional.of(new JsonValue.JsonBoolean(true));
            case "false" -> Optional.of(new JsonValue.JsonBoolean(false));
            case "null" -> Optional.of(JsonValue.NULL);
            default -> Optional.empty();
        };
    }

    private Token peek() {
        return tokens.get(cursor);
    }

    private Token next() {
        Token token = tokens.get(cursor);
        if (token.type() != TokenType.END) {
            cursor++;
        }
        return token;
    }

    private enum TokenType {
        OPEN_PAREN,
        CLOSE_PAREN,
        WORD,
        STRING,
        OPERATOR,
        END
    }

    private record Token(TokenType type, String text, int position) {
        boolean isKeyword(String keyword) {
            return type == TokenType.WORD && text.equalsIgnoreCase(keyword);
        }
    }

    private static final class Lexer {
        private final String text;
        private int position;

        Lexer(String text) {
            this.text = text;
        }

        List<Token> tokenize() {
            List<Token> tokens = new ArrayList<>();
            while (skipWhitespace()) {
                tokens.add(nextToken());
            }
            tokens.add(new Token(TokenType.END, "", text.length()));
            return tokens;
        }

        private boolean skipWhitespace() {
            while (position < text.length() && Character.isWhitespace(text.charAt(position))) {
                position++;
            }
            return position < text.length();
        }

        private Token nextToken() {
            int start = position;
            char c = text.charAt(position);
            return switch (c) {
                case '(' -> single(TokenType.OPEN_PAREN, start);
                case ')' -> single(TokenType.CLOSE_PAREN, start);
                case '"' -> string(start);
                case '=', '!', '<', '>' -> operator(start);
                default -> word(start);
            };
        }

        private Token single(TokenType type, int start) {
            position++;
            return new Token(type, text.substring(start, position), start);
        }

        private Token operator(int start) {
            position++;
            if (position < text.length() && text.charAt(position) == '=') {
                position++;
            }
            String symbol = text.substring(start, position);
            if (symbol.equals("!")) {
                throw new InvalidQueryException(Reason.UNEXPECTED_TOKEN, start, symbol);
            }
            return new Token(TokenType.OPERATOR, symbol, start);
        }

        private Token word(int start) {
            while (position < text.length() && isWordChar(text.charAt(position))) {
                position++;
            }
            if (position == start) {
                throw new InvalidQueryException(Reason.UNEXPECTED_TOKEN, start, String.valueOf(text.charAt(start)));
            }
            return new Token(TokenType.WORD, text.substring(start, position), start);
        }

        private Token string(int start) {
            StringBuilder value = new StringBuilder();
            position++;
            while (position < text.length()) {
                char c = text.charAt(position++);
                if (c == '"') {
                    return new Token(TokenType.STRING, value.toString(), start);
                }
                if (c == '\\' && position < text.length()) {
                    value.append(unescape(text.charAt(position++)));
                } else {
                    value.append(c);
                }
            }
            throw new InvalidQueryException(Reason.UNCLOSED_STRING, start, text.substring(start));
        }

        private static char unescape(char c) {
            return switch (c) {
                case 'n' -> '\n';
                case 't' -> '\t';
                case 'r' -> '\r';
                default -> c;
            };
        }

        private static boolean isWordChar(char c) {
            return Character.isLetterOrDigit(c) || "_$-+.[]".indexOf(c) >= 0;
        }
    }
}
