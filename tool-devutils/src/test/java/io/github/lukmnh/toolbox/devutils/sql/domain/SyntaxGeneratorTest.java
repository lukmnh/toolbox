package io.github.lukmnh.toolbox.devutils.sql.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class SyntaxGeneratorTest {

    private static final Runnable NO_CHECKPOINT = () -> {};

    /** id/kode dari CSV (RAW), active boolean, nama dengan kutip. */
    private static DataTable csvTable() {
        return new DataTable(
                "users.csv",
                List.of("id", "name", "code", "active"),
                List.of(
                        List.of(raw("1"), raw("O'Brien"), raw("00123"), raw("true")),
                        List.of(raw("2"), DataValue.NULL, raw("456"), raw("FALSE"))));
    }

    private static DataValue raw(String text) {
        return DataValue.raw(text);
    }

    private static String generate(DataTable table, SyntaxTarget target, DmlOperation operation, String key) {
        return SyntaxGenerator.generate(
                        table, new GenerateOptions(target, operation, "users", key, false), NO_CHECKPOINT)
                .text();
    }

    @Test
    void postgresInsertInfersTypesPerColumn() {
        assertEquals("""
                INSERT INTO users (id, name, code, active) VALUES (1, 'O''Brien', '00123', TRUE);
                INSERT INTO users (id, name, code, active) VALUES (2, NULL, '456', FALSE);
                """, generate(csvTable(), SyntaxTarget.POSTGRESQL, DmlOperation.INSERT, null));
    }

    @Test
    void oracleUpdateWritesBooleansAsNumbers() {
        assertEquals("""
                UPDATE users SET name = 'O''Brien', code = '00123', active = 1 WHERE id = 1;
                UPDATE users SET name = NULL, code = '456', active = 0 WHERE id = 2;
                """, generate(csvTable(), SyntaxTarget.ORACLE, DmlOperation.UPDATE, "id"));
    }

    @Test
    void deleteSkipsRowsWithEmptyKey() {
        GeneratedSyntax result = SyntaxGenerator.generate(
                csvTable(),
                new GenerateOptions(SyntaxTarget.POSTGRESQL, DmlOperation.DELETE, "users", "name", false),
                NO_CHECKPOINT);

        assertEquals("DELETE FROM users WHERE name = 'O''Brien';\n", result.text());
        assertEquals(1, result.rows());
        assertEquals(1, result.skippedRows());
    }

    @Test
    void mongoInsertManyKeepsTypedValues() {
        DataTable json = new DataTable(
                "data.json",
                List.of("id", "first name", "tags"),
                List.of(
                        List.of(DataValue.number("1"), DataValue.text("Budi \"B\""), DataValue.json("[\"a\"]")),
                        List.of(DataValue.text("2"), DataValue.NULL, DataValue.NULL)));

        assertEquals("""
                db.getCollection("users").insertMany([
                  { id: 1, "first name": "Budi \\"B\\"", tags: ["a"] },
                  { id: "2", "first name": null, tags: null }
                ]);
                """, generate(json, SyntaxTarget.MONGODB, DmlOperation.INSERT, null));
    }

    @Test
    void mongoUpdateAndDelete() {
        assertEquals(
                "db.getCollection(\"users\").updateOne({ id: 1 }, { $set: { name: \"O'Brien\", code: \"00123\", active:"
                        + " true } });\n"
                        + "db.getCollection(\"users\").updateOne({ id: 2 }, { $set: { name: null, code: \"456\", active:"
                        + " false } });\n",
                generate(csvTable(), SyntaxTarget.MONGODB, DmlOperation.UPDATE, "id"));
        assertEquals(
                "db.getCollection(\"users\").deleteOne({ id: 1 });\n"
                        + "db.getCollection(\"users\").deleteOne({ id: 2 });\n",
                generate(csvTable(), SyntaxTarget.MONGODB, DmlOperation.DELETE, "id"));
    }

    @Test
    void identifiersAreQuotedOnlyWhenNeeded() {
        DataTable table = new DataTable(
                "x.csv", List.of("First Name", "order", "Amount"), List.of(List.of(raw("A"), raw("1"), raw("2.50"))));

        String sql = SyntaxGenerator.generate(
                        table,
                        new GenerateOptions(
                                SyntaxTarget.POSTGRESQL, DmlOperation.INSERT, "sales.daily report", null, false),
                        NO_CHECKPOINT)
                .text();

        assertEquals(
                "INSERT INTO sales.\"daily report\" (\"First Name\", \"order\", Amount) VALUES ('A', 1, 2.50);\n", sql);
    }

    @Test
    void longDigitStringsStayText() {
        DataTable table = new DataTable("x.csv", List.of("nik"), List.of(List.of(raw("3201234567890001"))));

        assertEquals(
                "INSERT INTO users (nik) VALUES ('3201234567890001');\n",
                generate(table, SyntaxTarget.POSTGRESQL, DmlOperation.INSERT, null));
    }

    @Test
    void allTextQuotesNumbersAndBooleans() {
        String sql = SyntaxGenerator.generate(
                        csvTable(),
                        new GenerateOptions(SyntaxTarget.POSTGRESQL, DmlOperation.DELETE, "users", "id", true),
                        NO_CHECKPOINT)
                .text();

        assertEquals("DELETE FROM users WHERE id = '1';\nDELETE FROM users WHERE id = '2';\n", sql);
    }

    @Test
    void updateNeedsAColumnBesidesTheKey() {
        DataTable table = new DataTable("x.csv", List.of("id"), List.of(List.of(raw("1"))));
        GenerateOptions options = new GenerateOptions(SyntaxTarget.POSTGRESQL, DmlOperation.UPDATE, "t", "id", false);

        assertThrows(IllegalArgumentException.class, () -> SyntaxGenerator.generate(table, options, NO_CHECKPOINT));
    }

    @Test
    void unknownKeyColumnIsRejected() {
        GenerateOptions options = new GenerateOptions(SyntaxTarget.ORACLE, DmlOperation.DELETE, "t", "missing", false);

        assertThrows(
                IllegalArgumentException.class, () -> SyntaxGenerator.generate(csvTable(), options, NO_CHECKPOINT));
    }
}
