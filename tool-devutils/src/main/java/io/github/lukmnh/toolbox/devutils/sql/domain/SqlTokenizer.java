package io.github.lukmnh.toolbox.devutils.sql.domain;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Tokenizer SQL ringan, cukup untuk memecah statement dan minify tanpa merusak string literal, quoted identifier,
 * dan komentar. Bukan parser: keyword, angka, dan operator semuanya {@link SqlTokenType#WORD}.
 *
 * <p>Streaming (iterator) supaya file 10 MB tidak menghasilkan jutaan objek sekaligus. String/komentar yang tidak
 * ditutup dianggap berakhir di ujung input, sehingga input rusak tidak pernah membuat tokenizer gagal.
 */
public final class SqlTokenizer implements Iterator<SqlToken> {

    private static final String GO = "GO";

    private final String sql;
    private final SqlDialect dialect;
    private int position;

    public SqlTokenizer(String sql, SqlDialect dialect) {
        this.sql = sql;
        this.dialect = dialect;
    }

    @Override
    public boolean hasNext() {
        return position < sql.length();
    }

    @Override
    public SqlToken next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        int start = position;
        SqlTokenType type = scan();
        return new SqlToken(type, start, position);
    }

    /** Majukan {@link #position} melewati satu token dan kembalikan jenisnya. */
    private SqlTokenType scan() {
        char current = sql.charAt(position);
        if (Character.isWhitespace(current)) {
            position = skipWhitespace(position);
            return SqlTokenType.WHITESPACE;
        }
        if (current == ';') {
            position++;
            return SqlTokenType.SEMICOLON;
        }
        if (current == ',' || current == '(' || current == ')') {
            position++;
            return SqlTokenType.PUNCTUATION;
        }
        if (isLineCommentAt(position)) {
            position = lineEnd(position);
            return SqlTokenType.LINE_COMMENT;
        }
        if (isBlockCommentAt(position)) {
            position = blockCommentEnd(position);
            return SqlTokenType.BLOCK_COMMENT;
        }
        if (isBatchSeparatorAt(position)) {
            position += GO.length();
            return SqlTokenType.BATCH_SEPARATOR;
        }
        int quotedEnd = quotedEnd(position);
        if (quotedEnd > position) {
            SqlTokenType type = isIdentifierQuote(current) ? SqlTokenType.QUOTED_IDENTIFIER : SqlTokenType.STRING;
            position = quotedEnd;
            return type;
        }
        do {
            position++;
        } while (position < sql.length() && !startsToken(position));
        return SqlTokenType.WORD;
    }

    /** Karakter di {@code index} mengakhiri WORD yang sedang dibaca. */
    private boolean startsToken(int index) {
        char c = sql.charAt(index);
        return Character.isWhitespace(c)
                || c == ';'
                || c == ','
                || c == '('
                || c == ')'
                || isLineCommentAt(index)
                || isBlockCommentAt(index)
                || quotedEnd(index) > index;
    }

    private int skipWhitespace(int from) {
        int index = from;
        while (index < sql.length() && Character.isWhitespace(sql.charAt(index))) {
            index++;
        }
        return index;
    }

    private boolean isLineCommentAt(int index) {
        char c = sql.charAt(index);
        if (c == '#') {
            return dialect.supports(SqlSyntax.HASH_COMMENTS);
        }
        if (c != '-' || index + 1 >= sql.length() || sql.charAt(index + 1) != '-') {
            return false;
        }
        if (!dialect.supports(SqlSyntax.DASH_COMMENT_NEEDS_SPACE)) {
            return true;
        }
        // MySQL: "--" harus diikuti spasi/kontrol, supaya "a--1" tetap ekspresi aritmetika
        return index + 2 >= sql.length() || sql.charAt(index + 2) <= ' ';
    }

    private int lineEnd(int from) {
        int index = from;
        while (index < sql.length() && sql.charAt(index) != '\n' && sql.charAt(index) != '\r') {
            index++;
        }
        return index;
    }

    private boolean isBlockCommentAt(int index) {
        return sql.startsWith("/*", index);
    }

    private int blockCommentEnd(int from) {
        boolean nested = dialect.supports(SqlSyntax.NESTED_COMMENTS);
        int depth = 0;
        int index = from;
        while (index < sql.length()) {
            if (sql.startsWith("/*", index) && (depth == 0 || nested)) {
                depth++;
                index += 2;
            } else if (sql.startsWith("*/", index)) {
                depth--;
                index += 2;
                if (depth == 0) {
                    return index;
                }
            } else {
                index++;
            }
        }
        return sql.length();
    }

    /** {@code GO} sendirian di barisnya (boleh diapit spasi/tab). */
    private boolean isBatchSeparatorAt(int index) {
        if (!dialect.supports(SqlSyntax.GO_BATCH_SEPARATOR) || !sql.regionMatches(true, index, GO, 0, GO.length())) {
            return false;
        }
        return onlyBlanksBefore(index) && onlyBlanksAfter(index + GO.length());
    }

    private boolean onlyBlanksBefore(int index) {
        for (int i = index - 1; i >= 0; i--) {
            char c = sql.charAt(i);
            if (c == '\n' || c == '\r') {
                return true;
            }
            if (c != ' ' && c != '\t') {
                return false;
            }
        }
        return true;
    }

    private boolean onlyBlanksAfter(int index) {
        for (int i = index; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '\n' || c == '\r') {
                return true;
            }
            if (c != ' ' && c != '\t') {
                return false;
            }
        }
        return true;
    }

    /** Akhir string/quoted identifier yang dimulai di {@code index}, atau {@code index} bila bukan awal quote. */
    private int quotedEnd(int index) {
        char c = sql.charAt(index);
        boolean backslash = dialect.supports(SqlSyntax.BACKSLASH_ESCAPES);
        return switch (c) {
            case '\'', '"' -> quoteEnd(index + 1, c, backslash);
            case '`' -> quoteEnd(index + 1, c, false);
            case '[' -> dialect.supports(SqlSyntax.BRACKET_IDENTIFIERS) ? quoteEnd(index + 1, ']', false) : index;
            case '$' -> dialect.supports(SqlSyntax.DOLLAR_QUOTES) ? dollarQuoteEnd(index) : index;
            case 'E', 'e' ->
                isPrefixedQuote(index, SqlSyntax.ESCAPE_STRING_PREFIX) ? quoteEnd(index + 2, '\'', true) : index;
            case 'Q', 'q' -> isPrefixedQuote(index, SqlSyntax.ALTERNATIVE_QUOTING) ? alternativeQuoteEnd(index) : index;
            default -> index;
        };
    }

    /** {@code close} ganda ({@code ''}, {@code ""}, {@code ]]}) adalah escape, bukan penutup. */
    private int quoteEnd(int from, char close, boolean backslashEscapes) {
        int index = from;
        while (index < sql.length()) {
            char c = sql.charAt(index);
            if (backslashEscapes && c == '\\') {
                index += 2;
            } else if (c != close) {
                index++;
            } else if (index + 1 < sql.length() && sql.charAt(index + 1) == close) {
                index += 2;
            } else {
                return index + 1;
            }
        }
        return sql.length();
    }

    /** Prefix satu huruf ({@code E'..'}, {@code q'..'}) yang berdiri sendiri, bukan akhir identifier lain. */
    private boolean isPrefixedQuote(int index, SqlSyntax feature) {
        return dialect.supports(feature)
                && index + 1 < sql.length()
                && sql.charAt(index + 1) == '\''
                && !isIdentifierPartBefore(index);
    }

    /** {@code $$..$$} atau {@code $tag$..$tag$}; {@code $1} (parameter) bukan dollar quote. */
    private int dollarQuoteEnd(int index) {
        if (isIdentifierPartBefore(index)) {
            return index;
        }
        int tagEnd = index + 1;
        if (tagEnd < sql.length() && isTagStart(sql.charAt(tagEnd))) {
            while (tagEnd < sql.length() && isTagPart(sql.charAt(tagEnd))) {
                tagEnd++;
            }
        }
        if (tagEnd >= sql.length() || sql.charAt(tagEnd) != '$') {
            return index;
        }
        String tag = sql.substring(index, tagEnd + 1);
        int close = sql.indexOf(tag, tagEnd + 1);
        return close < 0 ? sql.length() : close + tag.length();
    }

    /** Oracle {@code q'[..]'}: penutup adalah pasangan delimiter lalu kutip. */
    private int alternativeQuoteEnd(int index) {
        int delimiterIndex = index + 2;
        if (delimiterIndex >= sql.length()) {
            return sql.length();
        }
        char closing = switch (sql.charAt(delimiterIndex)) {
            case '[' -> ']';
            case '{' -> '}';
            case '(' -> ')';
            case '<' -> '>';
            default -> sql.charAt(delimiterIndex);
        };
        int close = sql.indexOf(closing + "'", delimiterIndex + 1);
        return close < 0 ? sql.length() : close + 2;
    }

    /** {@code $} ikut dihitung karena identifier PostgreSQL boleh mengandung {@code $} ({@code a$b$}). */
    private boolean isIdentifierPartBefore(int index) {
        if (index == 0) {
            return false;
        }
        char previous = sql.charAt(index - 1);
        return isTagPart(previous) || previous == '$';
    }

    private static boolean isIdentifierQuote(char c) {
        return c == '"' || c == '`' || c == '[';
    }

    private static boolean isTagStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isTagPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
