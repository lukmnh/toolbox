package io.github.lukmnh.toolbox.devutils.sql.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.devutils.sql.application.FormatSqlUseCase;
import io.github.lukmnh.toolbox.devutils.sql.domain.FormatOptions;
import io.github.lukmnh.toolbox.devutils.sql.domain.IndentStyle;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlDialect;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class VerticalBlankSqlFormatterTest {

    private final VerticalBlankSqlFormatter formatter = new VerticalBlankSqlFormatter();

    private static FormatOptions options(SqlDialect dialect, IndentStyle indent, boolean uppercase) {
        return new FormatOptions(dialect, indent, uppercase, 1);
    }

    @ParameterizedTest
    @EnumSource(SqlDialect.class)
    void everyDialectFormatsSimpleQuery(SqlDialect dialect) {
        String formatted = formatter.format(
                "select id, email from customers where id = 1 order by email",
                options(dialect, IndentStyle.TWO_SPACES, true));
        assertTrue(formatted.startsWith("SELECT\n  id,\n  email\nFROM\n  customers\nWHERE"), formatted);
        assertEquals(formatted.strip(), formatted);
    }

    @Test
    void preserveCaseAndTabIndent() {
        assertEquals(
                "select\n\ta\nfrom\n\tt",
                formatter.format("select a from t", options(SqlDialect.STANDARD, IndentStyle.TAB, false)));
    }

    @Test
    void stringLiteralAndQuotedIdentifierAreUnchanged() {
        String formatted = formatter.format(
                "select [My  Col], 'a  b;c' from dbo.t", options(SqlDialect.TSQL, IndentStyle.FOUR_SPACES, true));
        assertTrue(formatted.contains("[My  Col]"), formatted);
        assertTrue(formatted.contains("'a  b;c'"), formatted);
        assertTrue(formatted.contains("\n    [My  Col]"), formatted);
    }

    /** Regresi: library kuadratik; seluruh skrip sekaligus butuh > 5 menit untuk 1,4 MB. */
    @Test
    void largeScriptStaysLinearThroughUseCase() {
        String statement = "select a.id, b.name from users a join orders b on a.id = b.uid where a.x in (1, 2);\n";
        String script = statement.repeat(10_000);
        FormatSqlUseCase useCase = new FormatSqlUseCase(formatter);
        String output = assertTimeout(
                Duration.ofSeconds(30),
                () -> useCase.execute(
                                script,
                                options(SqlDialect.MYSQL, IndentStyle.TWO_SPACES, true),
                                CancellationToken.none())
                        .text());
        assertEquals(10_000, output.split("SELECT\n", -1).length - 1);
    }
}
