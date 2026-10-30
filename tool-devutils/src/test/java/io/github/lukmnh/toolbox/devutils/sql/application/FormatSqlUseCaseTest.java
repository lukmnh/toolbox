package io.github.lukmnh.toolbox.devutils.sql.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.TaskCancelledException;
import io.github.lukmnh.toolbox.devutils.sql.domain.FormatOptions;
import io.github.lukmnh.toolbox.devutils.sql.domain.IndentStyle;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlDialect;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlFormatFailedException;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlOutput;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlStatementFormatter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FormatSqlUseCaseTest {

    private static final FormatOptions ONE_BLANK_LINE =
            new FormatOptions(SqlDialect.TSQL, IndentStyle.TWO_SPACES, true, 1);

    /** Fake formatter: upper-case + trim, mencatat statement yang diterima. */
    private static final class RecordingFormatter implements SqlStatementFormatter {

        final List<String> received = new ArrayList<>();

        @Override
        public String format(String statement, FormatOptions options) {
            received.add(statement);
            return statement.strip().toUpperCase();
        }
    }

    private final RecordingFormatter formatter = new RecordingFormatter();
    private final FormatSqlUseCase useCase = new FormatSqlUseCase(formatter);

    @Test
    void formatsEachStatementAndJoinsWithBlankLines() {
        SqlOutput output = useCase.execute("select 1; select 'a;b';", ONE_BLANK_LINE, CancellationToken.none());
        assertEquals("SELECT 1;\n\nSELECT 'A;B';\n", output.text());
        assertEquals(List.of("select 1;", " select 'a;b';"), formatter.received);
        assertEquals(0, output.unformattedStatements());
    }

    @Test
    void zeroBlankLinesPutsStatementsOnConsecutiveLines() {
        FormatOptions options = new FormatOptions(SqlDialect.STANDARD, IndentStyle.TAB, false, 0);
        assertEquals(
                "A;\nB;\n",
                useCase.execute("a; b;", options, CancellationToken.none()).text());
    }

    @Test
    void goSeparatorIsWrittenVerbatimWithoutFormatter() {
        SqlOutput output = useCase.execute("select 1\ngo\nselect 2", ONE_BLANK_LINE, CancellationToken.none());
        assertEquals("SELECT 1\n\ngo\n\nSELECT 2\n", output.text());
        assertEquals(2, formatter.received.size());
    }

    @Test
    void tooLongStatementIsKeptAsIs() {
        String huge = "select '" + "x".repeat(FormatSqlUseCase.MAX_STATEMENT_CHARS) + "';";
        SqlOutput output = useCase.execute("select 1; " + huge, ONE_BLANK_LINE, CancellationToken.none());
        assertEquals("SELECT 1;\n\n" + huge + "\n", output.text());
        assertEquals(1, output.unformattedStatements());
        assertEquals(1, formatter.received.size());
    }

    @Test
    void emptyInputGivesEmptyOutput() {
        assertEquals(
                "",
                useCase.execute("  \n ", ONE_BLANK_LINE, CancellationToken.none())
                        .text());
    }

    @Test
    void libraryErrorIsWrappedWithStatementNumber() {
        IllegalStateException libraryError = new IllegalStateException("isi SQL");
        FormatSqlUseCase failing = new FormatSqlUseCase((statement, options) -> {
            if (statement.contains("rusak")) {
                throw libraryError;
            }
            return statement.strip();
        });
        SqlFormatFailedException error = assertThrows(
                SqlFormatFailedException.class,
                () -> failing.execute("select 1; select rusak;", ONE_BLANK_LINE, CancellationToken.none()));
        assertEquals(2, error.statementNumber());
        assertSame(libraryError, error.getCause());
    }

    @Test
    void cancelledTokenStopsBeforeFormatting() {
        CancellationToken cancelled = new CancellationToken();
        cancelled.cancel();
        assertThrows(TaskCancelledException.class, () -> useCase.execute("select 1;", ONE_BLANK_LINE, cancelled));
        assertEquals(List.of(), formatter.received);
    }
}
