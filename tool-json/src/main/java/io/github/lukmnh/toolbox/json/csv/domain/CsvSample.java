package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Awal file untuk pratinjau: nama kolom input + maksimal {@link CsvLimits#PREVIEW_ROWS} record data.
 *
 * @param hasMore file masih punya record setelah sampel ini
 */
public record CsvSample(InputFormat format, List<String> columns, List<CsvRow> rows, boolean hasMore) {

    public CsvSample {
        Objects.requireNonNull(format, "format");
        columns = List.copyOf(columns);
        rows = List.copyOf(rows);
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Sample without columns");
        }
    }

    /** Tabel input apa adanya; record pendek diisi kosong hanya untuk tampilan, record panjang dipotong. */
    public TablePreview inputTable() {
        RowNormalizer normalizer = new RowNormalizer(columns.size(), MismatchPolicy.PAD);
        List<List<String>> table = new ArrayList<>(rows.size());
        for (CsvRow row : rows) {
            normalizer.normalize(row).ifPresent(table::add);
        }
        return new TablePreview(columns, table);
    }
}
