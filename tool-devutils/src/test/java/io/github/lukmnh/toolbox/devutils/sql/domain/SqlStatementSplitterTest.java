package io.github.lukmnh.toolbox.devutils.sql.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SqlStatementSplitterTest {

    @Test
    void splitsOnTopLevelSemicolonsOnly() {
        List<SqlStatement> statements =
                SqlStatementSplitter.split("select ';' from a; /* ; */ select 2;\n-- ;\nselect 3", SqlDialect.STANDARD);
        assertEquals(
                List.of(
                        new SqlStatement("select ';' from a;", false),
                        new SqlStatement(" /* ; */ select 2;", false),
                        new SqlStatement("\n-- ;\nselect 3", false)),
                statements);
    }

    @Test
    void blankSegmentsAreDropped() {
        assertEquals(
                List.of(new SqlStatement("select 1;", false)),
                SqlStatementSplitter.split("select 1;  \n\t ", SqlDialect.STANDARD));
        assertEquals(List.of(), SqlStatementSplitter.split("  \n\t", SqlDialect.STANDARD));
    }

    @Test
    void goLineBecomesSeparatorStatement() {
        assertEquals(
                List.of(
                        new SqlStatement("create table t (a int)\n", false),
                        new SqlStatement("GO", true),
                        new SqlStatement("\nselect 1", false)),
                SqlStatementSplitter.split("create table t (a int)\nGO\nselect 1", SqlDialect.TSQL));
    }

    @Test
    void dollarQuotedFunctionBodyStaysInOneStatement() {
        String function = "create function f() returns int as $$ begin return 1; end $$ language plpgsql;";
        assertEquals(
                List.of(new SqlStatement(function, false), new SqlStatement(" select f();", false)),
                SqlStatementSplitter.split(function + " select f();", SqlDialect.POSTGRESQL));
    }
}
