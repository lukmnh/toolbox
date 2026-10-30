package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.List;
import java.util.Objects;

/**
 * Gabungkan beberapa kolom (sesuai urutan {@code sourceColumns}) menjadi satu kolom baru.
 *
 * @param skipEmpty nilai kosong tidak ikut digabung, sehingga tidak ada pemisah ganda
 * @param removeSources kolom asal dihapus; kolom baru menempati posisi kolom asal pertama
 */
public record MergeStep(
        boolean enabled,
        List<String> sourceColumns,
        String separator,
        String newName,
        boolean skipEmpty,
        boolean removeSources)
        implements PipelineStep {

    public static final int MIN_SOURCES = 2;
    public static final String DEFAULT_SEPARATOR = " ";

    public MergeStep {
        sourceColumns = List.copyOf(sourceColumns);
        Objects.requireNonNull(separator, "separator");
        Objects.requireNonNull(newName, "newName");
    }

    public static MergeStep create(String newName) {
        return new MergeStep(true, List.of(), DEFAULT_SEPARATOR, newName, true, false);
    }

    @Override
    public MergeStep withEnabled(boolean enabled) {
        return new MergeStep(enabled, sourceColumns, separator, newName, skipEmpty, removeSources);
    }
}
