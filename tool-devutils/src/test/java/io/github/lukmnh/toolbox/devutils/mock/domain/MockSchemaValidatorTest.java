package io.github.lukmnh.toolbox.devutils.mock.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lukmnh.toolbox.devutils.mock.domain.InvalidMockSchemaException.Reason;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockSchemaValidatorTest {

    @Test
    void defaultOptionsAreValid() {
        List<MockColumn> columns = List.of(MockType.values()).stream()
                .map(type -> new MockColumn("c_" + type.name().toLowerCase(java.util.Locale.ROOT), type, ""))
                .toList();

        assertDoesNotThrow(() -> MockSchemaValidator.validate(schema(columns, 10, "users")));
    }

    @Test
    void rejectsInvalidColumnNameToPreventSqlInjection() {
        assertReason(Reason.INVALID_NAME, schema(List.of(column("name; DROP TABLE x", MockType.NAME, "")), 1, "t"));
    }

    @Test
    void rejectsDuplicateNamesIgnoringCase() {
        assertReason(
                Reason.DUPLICATE_NAME,
                schema(List.of(column("id", MockType.UUID, ""), column("ID", MockType.UUID, "")), 1, "t"));
    }

    @Test
    void rejectsReversedRange() {
        assertReason(Reason.INVALID_RANGE, schema(List.of(column("n", MockType.INTEGER, "10..1")), 1, "t"));
    }

    @Test
    void rejectsEnumWithoutValues() {
        assertReason(Reason.EMPTY_ENUM, schema(List.of(column("e", MockType.ENUM, " , ,")), 1, "t"));
    }

    @Test
    void rejectsRowsOutsideLimit() {
        assertReason(
                Reason.ROWS_OUT_OF_RANGE,
                schema(List.of(column("id", MockType.SEQUENCE, "")), MockSchemaValidator.MAX_ROWS + 1, "t"));
    }

    @Test
    void rejectsInvalidTableName() {
        assertReason(Reason.INVALID_TABLE, schema(List.of(column("id", MockType.SEQUENCE, "")), 1, "1users"));
    }

    private static void assertReason(Reason expected, MockSchema schema) {
        InvalidMockSchemaException error =
                assertThrows(InvalidMockSchemaException.class, () -> MockSchemaValidator.validate(schema));
        assertEquals(expected, error.reason());
    }

    private static MockColumn column(String name, MockType type, String options) {
        return new MockColumn(name, type, options);
    }

    private static MockSchema schema(List<MockColumn> columns, int rows, String table) {
        return new MockSchema(columns, MockLocale.ID, 1, rows, table);
    }
}
