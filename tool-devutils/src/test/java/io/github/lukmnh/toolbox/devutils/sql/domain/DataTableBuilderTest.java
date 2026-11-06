package io.github.lukmnh.toolbox.devutils.sql.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lukmnh.toolbox.devutils.sql.domain.InvalidDataException.Problem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DataTableBuilderTest {

    private final DataTableBuilder builder = new DataTableBuilder("data.csv", 2);

    @Test
    void trailingBlankHeadersAreDroppedAndShortRowsPadded() {
        builder.header(List.of(" id ", "name", "", ""), 1);
        builder.row(List.of(DataValue.raw("1")), 2);
        builder.row(List.of(DataValue.NULL, DataValue.NULL), 3);

        DataTable table = builder.build();

        assertEquals(List.of("id", "name"), table.columns());
        assertEquals(List.of(List.of(DataValue.raw("1"), DataValue.NULL)), table.rows());
    }

    @Test
    void duplicateColumnsIgnoringCaseAreRejected() {
        InvalidDataException error =
                assertThrows(InvalidDataException.class, () -> builder.header(List.of("Name", "name"), 1));

        assertEquals(Problem.DUPLICATE_COLUMN, error.problem());
        assertEquals("name", error.detail());
    }

    @Test
    void blankColumnInTheMiddleIsRejected() {
        InvalidDataException error =
                assertThrows(InvalidDataException.class, () -> builder.header(List.of("a", " ", "c"), 1));

        assertEquals(Problem.BLANK_COLUMN, error.problem());
        assertEquals("2", error.detail());
    }

    @Test
    void extraNonEmptyValueIsRejectedWithLineNumber() {
        builder.header(List.of("a"), 1);

        InvalidDataException error = assertThrows(
                InvalidDataException.class, () -> builder.row(List.of(DataValue.raw("1"), DataValue.raw("x")), 7));

        assertEquals(Problem.TOO_MANY_FIELDS, error.problem());
        assertEquals(7, error.line());
    }

    @Test
    void rowLimitIsEnforced() {
        builder.header(List.of("a"), 1);
        builder.row(List.of(DataValue.raw("1")), 2);
        builder.row(List.of(DataValue.raw("2")), 3);

        InvalidDataException error =
                assertThrows(InvalidDataException.class, () -> builder.row(List.of(DataValue.raw("3")), 4));

        assertEquals(Problem.TOO_MANY_ROWS, error.problem());
    }

    @Test
    void headerWithoutRowsIsRejected() {
        builder.header(List.of("a"), 1);

        assertEquals(
                Problem.NO_ROWS,
                assertThrows(InvalidDataException.class, builder::build).problem());
    }

    @Test
    void jsonRecordsAddColumnsInFirstSeenOrder() {
        Map<String, DataValue> first = new LinkedHashMap<>();
        first.put("id", DataValue.number("1"));
        Map<String, DataValue> second = new LinkedHashMap<>();
        second.put("name", DataValue.text("Sari"));
        second.put("id", DataValue.number("2"));
        builder.record(first, 1);
        builder.record(second, 2);

        DataTable table = builder.build();

        assertEquals(List.of("id", "name"), table.columns());
        assertEquals(
                List.of(DataValue.number("1"), DataValue.NULL), table.rows().get(0));
        assertEquals(
                List.of(DataValue.number("2"), DataValue.text("Sari")),
                table.rows().get(1));
    }

    @Test
    void nullTextBecomesNull() {
        assertEquals(DataValue.NULL, DataValue.raw(" null "));
        assertEquals(DataValue.NULL, DataValue.raw(""));
    }
}
