package io.github.lukmnh.toolbox.devutils.sql.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SqlMinifierTest {

    private static final Runnable NO_CHECKPOINT = () -> {};

    private static String minify(String sql, SqlDialect dialect, boolean removeComments) {
        return SqlMinifier.minify(sql, dialect, removeComments, NO_CHECKPOINT);
    }

    @Test
    void collapsesWhitespaceAndTightensPunctuation() {
        assertEquals(
                "SELECT a,b FROM t WHERE id IN (1,2);SELECT 1;",
                minify(
                        "SELECT a ,\n  b\nFROM   t\n\tWHERE id IN ( 1 , 2 ) ;\n\nSELECT 1;\n",
                        SqlDialect.STANDARD,
                        true));
    }

    @Test
    void keepsSpaceAroundParenthesesWhereSourceHadIt() {
        assertEquals(
                "select count (*) as n from t where a in (1,2) and (b=1 or c=2) order by n;",
                minify(
                        "select count ( * ) as n from t where a in ( 1, 2 ) and ( b=1 or c=2 ) order by n ;",
                        SqlDialect.STANDARD,
                        true));
    }

    @Test
    void stringLiteralsAndQuotedIdentifiersAreUntouched() {
        String sql = "select '  a\n  b  ', \"my   col\", `x  y`, [p  q] from t";
        assertEquals("select '  a\n  b  ',\"my   col\",`x  y`,[p  q] from t", minify(sql, SqlDialect.TSQL, true));
        assertEquals("select 'it''s  --  x'", minify("select   'it''s  --  x'", SqlDialect.STANDARD, true));
        assertEquals("select 'a\\'  b'", minify("select  'a\\'  b'", SqlDialect.MYSQL, true));
        assertEquals("select $$ a  ;  b $$", minify("select  $$ a  ;  b $$", SqlDialect.POSTGRESQL, true));
    }

    @Test
    void removedCommentStillSeparatesWords() {
        assertEquals("select a from t", minify("select/* c */a -- x\nfrom t", SqlDialect.STANDARD, true));
    }

    @Test
    void keptLineCommentEndsWithNewline() {
        assertEquals(
                "select a -- kolom a\nfrom t /* blok */ where b=1",
                minify("select a -- kolom a\n   from t /* blok */\n where b=1", SqlDialect.STANDARD, false));
    }

    @Test
    void hintCommentsAreAlwaysKept() {
        assertEquals(
                "select /*+ INDEX(t i) */ a from t /*!50001 x */",
                minify("select /*+ INDEX(t i) */ a /* buang */ from t /*!50001 x */", SqlDialect.MYSQL, true));
    }

    @Test
    void doubleMinusKeepsItsSpaceSoItDoesNotBecomeComment() {
        assertEquals("select a - -1", minify("select a  -  -1", SqlDialect.POSTGRESQL, true));
    }

    @Test
    void goStaysOnItsOwnLine() {
        assertEquals(
                "create table t (a int)\nGO\nselect 1",
                minify("create table t ( a int )\n\nGO\n\nselect 1\n", SqlDialect.TSQL, true));
    }

    @Test
    void emptyAndWhitespaceOnlyInput() {
        assertEquals("", minify("", SqlDialect.STANDARD, true));
        assertEquals("", minify(" \n\t ", SqlDialect.STANDARD, true));
        assertEquals("", minify("-- saja\n", SqlDialect.STANDARD, true));
    }

    @Test
    void checkpointCanCancel() {
        AtomicInteger calls = new AtomicInteger();
        Runnable cancelling = () -> {
            calls.incrementAndGet();
            throw new IllegalStateException("dibatalkan");
        };
        String large = "select 1 ;".repeat(5_000);
        assertThrows(
                IllegalStateException.class, () -> SqlMinifier.minify(large, SqlDialect.STANDARD, true, cancelling));
        assertEquals(1, calls.get());
    }
}
