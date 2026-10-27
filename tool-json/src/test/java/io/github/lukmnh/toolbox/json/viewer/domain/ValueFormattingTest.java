package io.github.lukmnh.toolbox.json.viewer.domain;

import static io.github.lukmnh.toolbox.json.viewer.domain.QueryParserTest.num;
import static io.github.lukmnh.toolbox.json.viewer.domain.QueryParserTest.object;
import static io.github.lukmnh.toolbox.json.viewer.domain.QueryParserTest.str;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** JsonWriter, CsvFormat, ByteSearch, SortKey, dan TableColumns. */
class ValueFormattingTest {

    private static final JsonValue NESTED =
            object("a", new JsonValue.JsonArray(List.of(num("1"), str("x\"y\n"))), "b", object(), "c", JsonValue.NULL);

    @Test
    void compactAndPrettyJson() {
        assertEquals("{\"a\":[1,\"x\\\"y\\n\"],\"b\":{},\"c\":null}", JsonWriter.compact(NESTED));
        assertEquals("""
                {
                  "a": [
                    1,
                    "x\\"y\\n"
                  ],
                  "b": {},
                  "c": null
                }""", JsonWriter.pretty(NESTED));
    }

    @Test
    void controlCharactersAreEscaped() {
        assertEquals("\"\\u0001\\t\"", JsonWriter.compact(str("\u0001\t")));
    }

    @Test
    void previewStopsAtLimitWithEllipsis() {
        List<JsonValue> many = new ArrayList<>(Collections.nCopies(10_000, num("12345")));
        String preview = JsonWriter.preview(new JsonValue.JsonArray(many), 200);

        assertEquals(201, preview.length());
        assertTrue(preview.endsWith(JsonWriter.ELLIPSIS));
    }

    @Test
    void displayTextShowsStringsWithoutQuotes() {
        assertEquals("Budi", str("Budi").displayText(200));
        assertEquals("null", JsonValue.NULL.displayText(200));
        assertEquals("{}", object().displayText(200));
    }

    @Test
    void csvEscapingFollowsRfc4180() {
        assertEquals("plain", CsvFormat.escape("plain"));
        assertEquals("\"a,b\"", CsvFormat.escape("a,b"));
        assertEquals("\"say \"\"hi\"\"\"", CsvFormat.escape("say \"hi\""));
        assertEquals("\"line1\r\nline2\"", CsvFormat.escape("line1\r\nline2"));
        assertEquals("a,\"b,c\",", CsvFormat.row(List.of("a", "b,c", "")));
    }

    @Test
    void csvCellsWriteNestedValuesAsJson() {
        assertEquals("", CsvFormat.cell(Optional.empty()));
        assertEquals("", CsvFormat.cell(Optional.of(JsonValue.NULL)));
        assertEquals("2.50", CsvFormat.cell(Optional.of(num("2.50"))));
        assertEquals(
                "{\"k\":[1]}", CsvFormat.cell(Optional.of(object("k", new JsonValue.JsonArray(List.of(num("1")))))));
    }

    @Test
    void quickSearchIsAsciiCaseInsensitiveOnRawBytes() {
        byte[] raw =
                "{\"nama\":\"BUDI Santoso\",\"kota\":\"Bekasi é\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(ByteSearch.contains(raw, ByteSearch.needle("budi s")));
        assertTrue(ByteSearch.contains(raw, ByteSearch.needle("bekasi é")));
        assertFalse(ByteSearch.contains(raw, ByteSearch.needle("jakarta")));
        assertTrue(ByteSearch.contains(raw, ByteSearch.needle("")));
    }

    @Test
    void sortKeysOrderNumbersNumericallyAndMissingLast() {
        List<SortKey> keys = new ArrayList<>(List.of(
                SortKey.of(Optional.of(num("10"))),
                SortKey.of(Optional.empty()),
                SortKey.of(Optional.of(num("9.5"))),
                SortKey.of(Optional.of(JsonValue.NULL)),
                SortKey.of(Optional.of(str("abc")))));

        keys.sort(SortKey.order(true));
        assertEquals(
                List.of("9.5", "10"),
                keys.subList(0, 2).stream().map(k -> k.number().toPlainString()).toList());
        assertEquals("abc", keys.get(2).text());
        assertTrue(keys.get(3).isMissing() && keys.get(4).isMissing());

        keys.sort(SortKey.order(false));
        assertEquals("abc", keys.get(0).text());
        assertEquals("10", keys.get(1).number().toPlainString());
        assertTrue(keys.get(3).isMissing() && keys.get(4).isMissing());
    }

    @Test
    void tableColumnsFollowKeyOrderAndAddValueColumnForScalars() {
        TableColumns columns = TableColumns.sample(
                List.of(object("id", num("1"), "nama", str("a")), object("id", num("2"), "email", str("e")), num("7")));

        assertEquals(
                List.of(
                        new RecordColumn.WholeValue(),
                        new RecordColumn.Field("id"),
                        new RecordColumn.Field("nama"),
                        new RecordColumn.Field("email")),
                columns.data());
        assertEquals(Optional.of(num("7")), new RecordColumn.WholeValue().extract(num("7")));
        assertTrue(new RecordColumn.WholeValue().extract(object("id", num("1"))).isEmpty());
    }
}
