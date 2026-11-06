package io.github.lukmnh.toolbox.devutils.sql.infrastructure;

import de.siegmar.fastcsv.reader.CsvParseException;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.FieldMismatchStrategy;
import io.github.lukmnh.toolbox.devutils.sql.domain.DataTable;
import io.github.lukmnh.toolbox.devutils.sql.domain.DataTableBuilder;
import io.github.lukmnh.toolbox.devutils.sql.domain.DataValue;
import io.github.lukmnh.toolbox.devutils.sql.domain.InvalidDataException;
import io.github.lukmnh.toolbox.devutils.sql.domain.InvalidDataException.Problem;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CSV (RFC 4180, delimiter dideteksi) dan TXT (tiap baris satu record dipisah koma, boleh diakhiri {@code .} atau
 * {@code ,}). Baris pertama = header; nilai di-trim.
 */
final class DelimitedDataReader {

    private static final char[] SEPARATORS = {',', ';', '\t', '|'};
    private static final char QUOTE = '"';

    private DelimitedDataReader() {}

    static DataTable csv(String fileName, String text, int maxRows) {
        return parse(fileName, text, detectSeparator(text), maxRows);
    }

    /** Satu {@code .} atau {@code ,} di ujung baris dibuang; jumlah baris tetap sama supaya nomor baris akurat. */
    static DataTable txt(String fileName, String text, int maxRows) {
        String records = text.lines()
                .map(String::stripTrailing)
                .map(line -> line.endsWith(".") || line.endsWith(",") ? line.substring(0, line.length() - 1) : line)
                .collect(Collectors.joining("\n"));
        return parse(fileName, records, ',', maxRows);
    }

    private static DataTable parse(String fileName, String text, char separator, int maxRows) {
        DataTableBuilder builder = new DataTableBuilder(fileName, maxRows);
        // Jumlah field yang tidak konsisten ditangani DataTableBuilder (isi NULL / tolak), bukan oleh parser
        try (CsvReader<CsvRecord> reader = CsvReader.builder()
                .fieldSeparator(separator)
                .skipEmptyLines(true)
                .extraFieldStrategy(FieldMismatchStrategy.IGNORE)
                .missingFieldStrategy(FieldMismatchStrategy.IGNORE)
                .ofCsvRecord(text)) {
            for (CsvRecord record : reader) {
                long line = record.getStartingLineNumber();
                List<String> fields = record.getFields();
                if (!builder.hasHeader()) {
                    builder.header(fields, line);
                } else {
                    builder.row(
                            fields.stream()
                                    .map(field -> DataValue.raw(field.strip()))
                                    .toList(),
                            line);
                }
            }
        } catch (CsvParseException | UncheckedIOException | IOException e) {
            throw new InvalidDataException(fileName, Problem.UNREADABLE, InvalidDataException.NO_LINE, "", e);
        }
        return builder.build();
    }

    /** Delimiter terbanyak di baris pertama yang tidak kosong (di luar tanda kutip); default koma. */
    static char detectSeparator(String text) {
        String header = text.lines().filter(line -> !line.isBlank()).findFirst().orElse("");
        char best = SEPARATORS[0];
        int bestCount = 0;
        for (char separator : SEPARATORS) {
            int count = 0;
            boolean quoted = false;
            for (int index = 0; index < header.length(); index++) {
                char c = header.charAt(index);
                if (c == QUOTE) {
                    quoted = !quoted;
                } else if (c == separator && !quoted) {
                    count++;
                }
            }
            if (count > bestCount) {
                best = separator;
                bestCount = count;
            }
        }
        return best;
    }
}
