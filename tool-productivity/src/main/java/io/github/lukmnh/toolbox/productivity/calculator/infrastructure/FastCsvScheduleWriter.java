package io.github.lukmnh.toolbox.productivity.calculator.infrastructure;

import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;
import io.github.lukmnh.toolbox.common.security.CsvFormulaGuard;
import io.github.lukmnh.toolbox.productivity.calculator.domain.ScheduleCsvWriter;
import io.github.lukmnh.toolbox.productivity.calculator.domain.ScheduleExportException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** CSV RFC 4180 lewat FastCSV, dengan BOM UTF-8 dan CRLF supaya Excel Windows membacanya dengan benar. */
public final class FastCsvScheduleWriter implements ScheduleCsvWriter {

    private static final char BYTE_ORDER_MARK = '﻿';

    @Override
    public void write(Path target, List<String> header, List<List<String>> rows) {
        try (Writer output = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            // Tanpa BOM, Excel membuka CSV dengan code page lokal dan teks non-ASCII jadi rusak
            output.write(BYTE_ORDER_MARK);
            try (CsvWriter csv =
                    CsvWriter.builder().lineDelimiter(LineDelimiter.CRLF).build(output)) {
                csv.writeRecord(guarded(header));
                for (List<String> row : rows) {
                    csv.writeRecord(guarded(row));
                }
            }
        } catch (IOException | UncheckedIOException e) {
            throw new ScheduleExportException(target.getFileName().toString(), e);
        }
    }

    private static List<String> guarded(List<String> cells) {
        return cells.stream().map(CsvFormulaGuard::neutralize).toList();
    }
}
