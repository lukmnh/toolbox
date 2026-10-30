package io.github.lukmnh.toolbox.devutils.mock.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class MockRendererTest {

    private static final MockSchema SCHEMA = new MockSchema(
            List.of(
                    new MockColumn("id", MockType.SEQUENCE, ""),
                    new MockColumn("note", MockType.ENUM, "x"),
                    new MockColumn("ok", MockType.BOOLEAN, "")),
            MockLocale.EN,
            1,
            2,
            "items");
    private static final List<List<String>> ROWS =
            List.of(List.of("1", "it's \"a\", b", "true"), List.of("2", "=SUM(A1)", "false"));

    @Test
    void rendersJsonWithTypedValuesAndEscaping() throws Exception {
        assertEquals("""
                [
                  {"id": 1, "note": "it's \\"a\\", b", "ok": true},
                  {"id": 2, "note": "=SUM(A1)", "ok": false}
                ]
                """, render(MockFormat.JSON, UnaryOperator.identity()));
    }

    @Test
    void rendersCsvWithQuotingAndFormulaGuard() throws Exception {
        String csv = render(MockFormat.CSV, cell -> cell.startsWith("=") ? "'" + cell : cell);

        assertEquals("id,note,ok\r\n1,\"it's \"\"a\"\", b\",true\r\n2,'=SUM(A1),false\r\n", csv);
    }

    @Test
    void rendersSqlInsertsWithEscapedStrings() throws Exception {
        assertEquals(
                "INSERT INTO items (id, note, ok) VALUES (1, 'it''s \"a\", b', true);\n"
                        + "INSERT INTO items (id, note, ok) VALUES (2, '=SUM(A1)', false);\n",
                render(MockFormat.SQL, UnaryOperator.identity()));
    }

    @Test
    void emptyJsonIsAnEmptyArray() throws Exception {
        StringBuilder out = new StringBuilder();
        new MockRenderer(UnaryOperator.identity())
                .render(SCHEMA, MockFormat.JSON, List.<List<String>>of().iterator(), 10, out, () -> {});

        assertEquals("[]\n", out.toString());
    }

    @Test
    void checkpointRunsEveryThousandRows() throws Exception {
        List<List<String>> rows = java.util.Collections.nCopies(2_500, List.of("1", "x", "true"));
        AtomicInteger checkpoints = new AtomicInteger();
        new MockRenderer(UnaryOperator.identity())
                .render(
                        SCHEMA,
                        MockFormat.SQL,
                        rows.iterator(),
                        rows.size(),
                        new StringBuilder(),
                        checkpoints::incrementAndGet);

        assertEquals(2, checkpoints.get());
    }

    private static String render(MockFormat format, UnaryOperator<String> csvCell) throws Exception {
        StringBuilder out = new StringBuilder();
        new MockRenderer(csvCell).render(SCHEMA, format, ROWS.iterator(), ROWS.size(), out, () -> {});
        return out.toString();
    }
}
