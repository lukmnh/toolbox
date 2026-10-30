package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.List;

/**
 * Satu record CSV mentah.
 *
 * @param lineNumber nomor baris awal record di file (mulai 1); record ber-quote bisa melintasi beberapa baris
 */
public record CsvRow(long lineNumber, List<String> fields) {

    public CsvRow {
        fields = List.copyOf(fields);
        if (lineNumber < 1) {
            throw new IllegalArgumentException("Line numbers start at 1");
        }
    }
}
