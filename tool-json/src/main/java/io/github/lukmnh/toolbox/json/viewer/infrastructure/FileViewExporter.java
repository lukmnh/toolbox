package io.github.lukmnh.toolbox.json.viewer.infrastructure;

import io.github.lukmnh.toolbox.common.security.CsvFormulaGuard;
import io.github.lukmnh.toolbox.json.shared.domain.JsonIoException;
import io.github.lukmnh.toolbox.json.shared.domain.ScanProgress;
import io.github.lukmnh.toolbox.json.viewer.domain.CsvFormat;
import io.github.lukmnh.toolbox.json.viewer.domain.ExportFormat;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonValue;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonWriter;
import io.github.lukmnh.toolbox.json.viewer.domain.RecordColumn;
import io.github.lukmnh.toolbox.json.viewer.domain.RecordSource;
import io.github.lukmnh.toolbox.json.viewer.domain.ViewExporter;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Export streaming: record dibaca satu per satu dari file sumber, tidak ditampung di memori. */
public final class FileViewExporter implements ViewExporter {

    private static final int OUTPUT_BUFFER_BYTES = 1 << 16;
    private static final int PROGRESS_INTERVAL_ROWS = 1024;

    @Override
    public void export(
            RecordSource source,
            int[] rows,
            List<RecordColumn> columns,
            ExportFormat format,
            Path output,
            ScanProgress progress) {
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(output), OUTPUT_BUFFER_BYTES)) {
            switch (format) {
                case JSON_ARRAY -> writeJsonArray(source, rows, out, progress);
                case JSON_LINES -> writeJsonLines(source, rows, out, progress);
                case CSV -> writeCsv(source, rows, columns, out, progress);
            }
            progress.update(rows.length, rows.length);
        } catch (IOException e) {
            throw new JsonIoException(output, e);
        }
    }

    /** Byte record disalin apa adanya, sehingga format angka dan urutan key tidak berubah. */
    private static void writeJsonArray(RecordSource source, int[] rows, OutputStream out, ScanProgress progress)
            throws IOException {
        write(out, "[");
        for (int i = 0; i < rows.length; i++) {
            write(out, i == 0 ? "\n" : ",\n");
            out.write(source.raw(rows[i]));
            report(i, rows.length, progress);
        }
        write(out, rows.length == 0 ? "]\n" : "\n]\n");
    }

    /** Record yang ditulis multi-baris di file sumber diringkas ke satu baris. */
    private static void writeJsonLines(RecordSource source, int[] rows, OutputStream out, ScanProgress progress)
            throws IOException {
        for (int i = 0; i < rows.length; i++) {
            byte[] raw = source.raw(rows[i]);
            if (containsLineBreak(raw)) {
                write(out, JsonWriter.compact(source.parse(raw)));
            } else {
                out.write(raw);
            }
            write(out, "\n");
            report(i, rows.length, progress);
        }
    }

    private static void writeCsv(
            RecordSource source, int[] rows, List<RecordColumn> columns, OutputStream out, ScanProgress progress)
            throws IOException {
        // Header (key JSON) dan nilai sama-sama berasal dari file tidak dipercaya, jadi keduanya dinetralisir (#18)
        List<String> headers = columns.stream()
                .map(column -> CsvFormulaGuard.neutralize(column.header()))
                .toList();
        write(out, CsvFormat.row(headers) + CsvFormat.LINE_END);
        for (int i = 0; i < rows.length; i++) {
            JsonValue record = source.parse(source.raw(rows[i]));
            List<String> cells = columns.stream()
                    .map(column -> CsvFormulaGuard.neutralize(CsvFormat.cell(column.extract(record))))
                    .toList();
            write(out, CsvFormat.row(cells) + CsvFormat.LINE_END);
            report(i, rows.length, progress);
        }
    }

    private static boolean containsLineBreak(byte[] raw) {
        for (byte value : raw) {
            if (value == '\n' || value == '\r') {
                return true;
            }
        }
        return false;
    }

    private static void write(OutputStream out, String text) throws IOException {
        out.write(text.getBytes(StandardCharsets.UTF_8));
    }

    private static void report(int index, int total, ScanProgress progress) {
        if ((index + 1) % PROGRESS_INTERVAL_ROWS == 0) {
            progress.update(index + 1L, total);
        }
    }
}
