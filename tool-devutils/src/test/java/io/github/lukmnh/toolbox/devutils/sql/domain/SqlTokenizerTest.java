package io.github.lukmnh.toolbox.devutils.sql.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SqlTokenizerTest {

    /** Token non-whitespace sebagai "TYPE:teks". */
    private static List<String> tokens(String sql, SqlDialect dialect) {
        List<String> result = new ArrayList<>();
        SqlTokenizer tokenizer = new SqlTokenizer(sql, dialect);
        while (tokenizer.hasNext()) {
            SqlToken token = tokenizer.next();
            if (token.type() != SqlTokenType.WHITESPACE) {
                result.add(token.type() + ":" + token.text(sql));
            }
        }
        return result;
    }

    @Test
    void splitsWordsPunctuationAndSemicolon() {
        assertEquals(
                List.of(
                        "WORD:select",
                        "WORD:count",
                        "PUNCTUATION:(",
                        "WORD:*",
                        "PUNCTUATION:)",
                        "PUNCTUATION:,",
                        "WORD:a>=1",
                        "SEMICOLON:;"),
                tokens("select count(*), a>=1;", SqlDialect.STANDARD));
    }

    @Test
    void tokensCoverInputWithoutGaps() {
        String sql = "select 'a;b' -- c\n/* d */ \"e\" from t;";
        SqlTokenizer tokenizer = new SqlTokenizer(sql, SqlDialect.STANDARD);
        StringBuilder rebuilt = new StringBuilder();
        while (tokenizer.hasNext()) {
            rebuilt.append(tokenizer.next().text(sql));
        }
        assertEquals(sql, rebuilt.toString());
    }

    @Test
    void stringWithDoubledQuoteAndSemicolonIsOneToken() {
        assertEquals(
                List.of("WORD:select", "STRING:'it''s; -- not comment'"),
                tokens("select 'it''s; -- not comment'", SqlDialect.STANDARD));
    }

    @Test
    void backslashEscapeOnlyInMysqlFamily() {
        assertEquals(List.of("STRING:'a\\'b'", "WORD:x"), tokens("'a\\'b' x", SqlDialect.MYSQL));
        // Standard SQL: backslash biasa, jadi string berakhir di kutip kedua
        assertEquals(List.of("STRING:'a\\'", "WORD:b", "STRING:' x"), tokens("'a\\'b' x", SqlDialect.STANDARD));
    }

    @Test
    void quotedIdentifiers() {
        assertEquals(
                List.of("QUOTED_IDENTIFIER:\"my \"\"col\"\"\"", "QUOTED_IDENTIFIER:`a``b`"),
                tokens("\"my \"\"col\"\"\" `a``b`", SqlDialect.MYSQL));
        assertEquals(List.of("QUOTED_IDENTIFIER:[my ]]col]"), tokens("[my ]]col]", SqlDialect.TSQL));
        // Di PostgreSQL "[" adalah subscript array, bukan quote
        assertEquals(List.of("WORD:a[1]"), tokens("a[1]", SqlDialect.POSTGRESQL));
    }

    @Test
    void lineAndBlockComments() {
        assertEquals(
                List.of("LINE_COMMENT:-- satu; dua", "WORD:x", "BLOCK_COMMENT:/* a; b */", "WORD:y"),
                tokens("-- satu; dua\nx /* a; b */ y", SqlDialect.STANDARD));
    }

    @Test
    void mysqlDashCommentNeedsSpaceAndHashIsComment() {
        assertEquals(List.of("WORD:a--1"), tokens("a--1", SqlDialect.MYSQL));
        assertEquals(List.of("WORD:a", "LINE_COMMENT:-- c"), tokens("a -- c", SqlDialect.MYSQL));
        assertEquals(List.of("WORD:a", "LINE_COMMENT:# c"), tokens("a # c", SqlDialect.MYSQL));
        assertEquals(List.of("WORD:a", "WORD:#", "WORD:c"), tokens("a # c", SqlDialect.POSTGRESQL));
    }

    @Test
    void nestedBlockCommentsInPostgresql() {
        assertEquals(
                List.of("BLOCK_COMMENT:/* a /* b */ c */", "WORD:x"),
                tokens("/* a /* b */ c */ x", SqlDialect.POSTGRESQL));
        assertEquals(
                List.of("BLOCK_COMMENT:/* a /* b */", "WORD:c", "WORD:*/"),
                tokens("/* a /* b */ c */", SqlDialect.MYSQL));
    }

    @Test
    void dollarQuotedBodyInPostgresql() {
        assertEquals(
                List.of("WORD:as", "STRING:$fn$ begin; 'x'; end $fn$", "SEMICOLON:;"),
                tokens("as $fn$ begin; 'x'; end $fn$;", SqlDialect.POSTGRESQL));
        assertEquals(List.of("STRING:$$a;b$$"), tokens("$$a;b$$", SqlDialect.POSTGRESQL));
        // $1 adalah parameter, bukan dollar quote
        assertEquals(List.of("WORD:=$1", "SEMICOLON:;"), tokens("=$1;", SqlDialect.POSTGRESQL));
    }

    @Test
    void escapeStringPrefixInPostgresql() {
        assertEquals(List.of("STRING:E'a\\'b'"), tokens("E'a\\'b'", SqlDialect.POSTGRESQL));
        // Bukan prefix bila menempel di identifier lain
        assertEquals(List.of("WORD:somE", "STRING:'x'"), tokens("somE'x'", SqlDialect.POSTGRESQL));
    }

    @Test
    void oracleAlternativeQuoting() {
        assertEquals(List.of("STRING:q'[it's]'", "WORD:x"), tokens("q'[it's]' x", SqlDialect.PLSQL));
        assertEquals(List.of("STRING:Q'!a'b!'"), tokens("Q'!a'b!'", SqlDialect.PLSQL));
    }

    @Test
    void goBatchSeparatorOnlyOnItsOwnLine() {
        assertEquals(
                List.of("WORD:select", "WORD:1", "BATCH_SEPARATOR:GO", "WORD:select", "WORD:2"),
                tokens("select 1\n  GO  \nselect 2", SqlDialect.TSQL));
        assertEquals(List.of("WORD:select", "WORD:go"), tokens("select go", SqlDialect.TSQL));
        assertEquals(List.of("WORD:GO"), tokens("GO", SqlDialect.STANDARD));
    }

    @Test
    void unterminatedStringAndCommentRunToEnd() {
        assertEquals(List.of("WORD:select", "STRING:'abc"), tokens("select 'abc", SqlDialect.STANDARD));
        assertEquals(List.of("BLOCK_COMMENT:/* abc"), tokens("/* abc", SqlDialect.STANDARD));
        assertEquals(List.of("STRING:'a\\"), tokens("'a\\", SqlDialect.MYSQL));
    }
}
