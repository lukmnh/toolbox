package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.List;

/**
 * Laporan ringkas record tidak konsisten.
 *
 * @param entries maksimal {@link CsvLimits#MAX_MISMATCH_ENTRIES} entri pertama
 * @param total jumlah seluruh record tidak konsisten
 * @param expectedColumns lebar tabel acuan
 */
public record MismatchReport(List<RowMismatch> entries, long total, int expectedColumns, MismatchPolicy policy) {

    public MismatchReport {
        entries = List.copyOf(entries);
    }

    public boolean isEmpty() {
        return total == 0;
    }

    public boolean isTruncated() {
        return total > entries.size();
    }
}
