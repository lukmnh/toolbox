package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Menyamakan lebar setiap record dengan lebar tabel sesuai {@link MismatchPolicy}, sambil mencatat laporan. Dipakai
 * pratinjau dan ekspor supaya hasil keduanya identik. Tidak thread-safe: satu instance per proses baca.
 */
public final class RowNormalizer {

    private final int width;
    private final MismatchPolicy policy;
    private final List<RowMismatch> entries = new ArrayList<>();
    private long total;

    public RowNormalizer(int width, MismatchPolicy policy) {
        if (width < 1) {
            throw new IllegalArgumentException("Table width must be at least 1");
        }
        this.width = width;
        this.policy = policy;
    }

    /** @return kosong bila record dilewati */
    public Optional<List<String>> normalize(CsvRow row) {
        List<String> fields = row.fields();
        if (fields.size() == width) {
            return Optional.of(fields);
        }
        record(row.lineNumber(), fields.size());
        return policy == MismatchPolicy.SKIP ? Optional.empty() : Optional.of(resize(fields));
    }

    public MismatchReport report() {
        return new MismatchReport(entries, total, width, policy);
    }

    private void record(long lineNumber, int fieldCount) {
        total++;
        if (entries.size() < CsvLimits.MAX_MISMATCH_ENTRIES) {
            entries.add(new RowMismatch(lineNumber, fieldCount));
        }
    }

    private List<String> resize(List<String> fields) {
        if (fields.size() > width) {
            return fields.subList(0, width);
        }
        List<String> padded = new ArrayList<>(width);
        padded.addAll(fields);
        while (padded.size() < width) {
            padded.add("");
        }
        return padded;
    }
}
