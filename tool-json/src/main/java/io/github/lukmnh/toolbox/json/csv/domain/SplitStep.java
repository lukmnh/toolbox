package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.List;
import java.util.Objects;

/**
 * Pecah satu kolom menjadi beberapa kolom baru yang disisipkan setelah kolom asal.
 *
 * @param resultNames nama kolom hasil; yang kosong/kurang diberi nama {@code <asal>_<n>}
 * @param maxColumns jumlah maksimum kolom hasil; sisa teks digabung ke kolom terakhir
 */
public record SplitStep(
        boolean enabled,
        String sourceColumn,
        SplitMethod method,
        List<String> resultNames,
        int maxColumns,
        boolean keepSource)
        implements PipelineStep {

    public static final int MIN_COLUMNS = 2;
    public static final int MAX_COLUMNS = 50;
    public static final int DEFAULT_COLUMNS = 2;
    public static final String DEFAULT_SEPARATOR = " ";

    public SplitStep {
        Objects.requireNonNull(sourceColumn, "sourceColumn");
        Objects.requireNonNull(method, "method");
        resultNames = List.copyOf(resultNames);
        if (maxColumns < MIN_COLUMNS || maxColumns > MAX_COLUMNS) {
            throw new IllegalArgumentException("Max output columns outside " + MIN_COLUMNS + ".." + MAX_COLUMNS);
        }
    }

    /** Langkah baru: pecah {@code sourceColumn} di spasi menjadi dua kolom. */
    public static SplitStep create(String sourceColumn) {
        return new SplitStep(
                true, sourceColumn, new SplitMethod.BySeparator(DEFAULT_SEPARATOR), List.of(), DEFAULT_COLUMNS, false);
    }

    @Override
    public SplitStep withEnabled(boolean enabled) {
        return new SplitStep(enabled, sourceColumn, method, resultNames, maxColumns, keepSource);
    }
}
