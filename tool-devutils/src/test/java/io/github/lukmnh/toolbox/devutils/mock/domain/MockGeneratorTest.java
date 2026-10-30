package io.github.lukmnh.toolbox.devutils.mock.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockGeneratorTest {

    private static final int ROWS = 500;

    @Test
    void sameSeedProducesSameRows() {
        assertEquals(collect(schema(42)), collect(schema(42)));
    }

    @Test
    void differentSeedProducesDifferentRows() {
        assertNotEquals(collect(schema(42)), collect(schema(43)));
    }

    @Test
    void producesExactRowCountAndSequence() {
        List<List<String>> rows = collect(schema(1));

        assertEquals(ROWS, rows.size());
        assertEquals("1", rows.getFirst().get(0));
        assertEquals(String.valueOf(ROWS), rows.getLast().get(0));
    }

    @Test
    void valuesRespectTheirOptions() {
        for (List<String> row : collect(schema(7))) {
            long integer = Long.parseLong(row.get(1));
            assertTrue(integer >= 10 && integer <= 20, row.get(1));
            LocalDate date = LocalDate.parse(row.get(2));
            assertTrue(!date.isBefore(LocalDate.of(2024, 1, 1)) && !date.isAfter(LocalDate.of(2024, 1, 31)));
            assertTrue(List.of("red", "green").contains(row.get(3)), row.get(3));
            assertTrue(row.get(4).matches("INV-\\d{4}-[A-Z]"), row.get(4));
            assertTrue(row.get(5).matches("[a-z.]+\\d{1,2}@example\\.(com|org|net)"), row.get(5));
            assertTrue(row.get(6).matches("\\d+\\.\\d{2}"), row.get(6));
        }
    }

    private static MockSchema schema(long seed) {
        return new MockSchema(
                List.of(
                        new MockColumn("id", MockType.SEQUENCE, ""),
                        new MockColumn("qty", MockType.INTEGER, "10..20"),
                        new MockColumn("day", MockType.DATE, "2024-01-01..2024-01-31"),
                        new MockColumn("color", MockType.ENUM, "red, green"),
                        new MockColumn("code", MockType.PATTERN, "INV-####-?"),
                        new MockColumn("email", MockType.EMAIL, ""),
                        new MockColumn("price", MockType.DECIMAL, "1..5")),
                MockLocale.EN,
                seed,
                ROWS,
                "items");
    }

    private static List<List<String>> collect(MockSchema schema) {
        List<List<String>> rows = new ArrayList<>();
        Iterator<List<String>> iterator = MockGenerator.rows(schema);
        iterator.forEachRemaining(rows::add);
        return rows;
    }
}
