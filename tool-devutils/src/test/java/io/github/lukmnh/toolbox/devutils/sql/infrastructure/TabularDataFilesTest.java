package io.github.lukmnh.toolbox.devutils.sql.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lukmnh.toolbox.devutils.sql.domain.DataTable;
import io.github.lukmnh.toolbox.devutils.sql.domain.DataValue;
import io.github.lukmnh.toolbox.devutils.sql.domain.InvalidDataException;
import io.github.lukmnh.toolbox.devutils.sql.domain.InvalidDataException.Problem;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlFileTooLargeException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TabularDataFilesTest {

    private static final int MAX_ROWS = 100;

    @TempDir
    Path dir;

    private final TabularDataFiles files = new TabularDataFiles(1024 * 1024, 10 * 1024 * 1024, MAX_ROWS);

    private Path write(String name, String content) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void csvDetectsSemicolonAndTrimsValues() throws IOException {
        DataTable table = files.read(write("users.csv", "﻿id;name\n1; Budi \n2;\"A;B\"\n"));

        assertEquals("users.csv", table.sourceName());
        assertEquals(List.of("id", "name"), table.columns());
        assertEquals(
                List.of(DataValue.raw("1"), DataValue.raw("Budi")), table.rows().get(0));
        assertEquals(DataValue.raw("A;B"), table.rows().get(1).get(1));
    }

    @Test
    void txtStripsTrailingPeriodOrComma() throws IOException {
        DataTable table =
                files.read(write("data.txt", "id, name, city.\n1, Budi, Jakarta.\n\n2, Sari, Jl. Merdeka,\n"));

        assertEquals(List.of("id", "name", "city"), table.columns());
        assertEquals(
                List.of(DataValue.raw("2"), DataValue.raw("Sari"), DataValue.raw("Jl. Merdeka")),
                table.rows().get(1));
    }

    @Test
    void csvRowWithExtraValueReportsLine() throws IOException {
        Path file = write("bad.csv", "a,b\n1,2\n3,4,5\n");

        InvalidDataException error = assertThrows(InvalidDataException.class, () -> files.read(file));

        assertEquals(Problem.TOO_MANY_FIELDS, error.problem());
        assertEquals(3, error.line());
    }

    @Test
    void jsonKeepsTypesAndNestedValues() throws IOException {
        DataTable table = files.read(write(
                "users.json",
                "[{\"id\": 1, \"name\": \"Budi\", \"tags\": [\"a\", \"b\"]}, {\"id\": 2, \"ok\": true}]"));

        assertEquals(List.of("id", "name", "tags", "ok"), table.columns());
        assertEquals(
                List.of(DataValue.number("1"), DataValue.text("Budi"), DataValue.json("[\"a\",\"b\"]"), DataValue.NULL),
                table.rows().get(0));
        assertEquals(DataValue.bool(true), table.rows().get(1).get(3));
    }

    @Test
    void jsonArrayOfScalarsIsRejected() throws IOException {
        Path file = write("bad.json", "[1, 2]");

        assertEquals(
                Problem.NOT_OBJECTS,
                assertThrows(InvalidDataException.class, () -> files.read(file)).problem());
    }

    @Test
    void xlsxReadsTypedCellsAndDates() throws IOException {
        Path file = dir.resolve("orders.xlsx");
        try (OutputStream out = Files.newOutputStream(file)) {
            Workbook workbook = new Workbook(out, "test", "1.0");
            Worksheet sheet = workbook.newWorksheet("Orders");
            sheet.value(0, 0, "id");
            sheet.value(0, 1, "code");
            sheet.value(0, 2, "paid");
            sheet.value(0, 3, "date");
            sheet.value(1, 0, 10);
            sheet.value(1, 1, "00123");
            sheet.value(1, 2, true);
            sheet.value(1, 3, LocalDate.of(2026, 11, 6));
            sheet.style(1, 3).format("yyyy-mm-dd").set();
            sheet.value(2, 0, 2.5);
            workbook.finish();
        }

        DataTable table = files.read(file);

        assertEquals(List.of("id", "code", "paid", "date"), table.columns());
        assertEquals(
                List.of(
                        DataValue.number("10"),
                        DataValue.text("00123"),
                        DataValue.bool(true),
                        DataValue.text("2026-11-06")),
                table.rows().get(0));
        assertEquals(
                List.of(DataValue.number("2.5"), DataValue.NULL, DataValue.NULL, DataValue.NULL),
                table.rows().get(1));
    }

    @Test
    void corruptXlsxIsUnreadable() throws IOException {
        Path file = write("broken.xlsx", "not a zip");

        assertEquals(
                Problem.UNREADABLE,
                assertThrows(InvalidDataException.class, () -> files.read(file)).problem());
    }

    @Test
    void unsupportedExtensionIsRejected() throws IOException {
        Path file = write("data.xls", "x");

        InvalidDataException error = assertThrows(InvalidDataException.class, () -> files.read(file));

        assertEquals(Problem.UNSUPPORTED_FORMAT, error.problem());
        assertEquals("xls", error.detail());
    }

    @Test
    void oversizedFileIsRejected() throws IOException {
        TabularDataFiles tiny = new TabularDataFiles(4, 1024, MAX_ROWS);
        Path file = write("big.csv", "a,b\n1,2\n");

        assertThrows(SqlFileTooLargeException.class, () -> tiny.read(file));
    }
}
