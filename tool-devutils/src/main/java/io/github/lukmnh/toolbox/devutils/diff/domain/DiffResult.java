package io.github.lukmnh.toolbox.devutils.diff.domain;

import java.util.List;
import java.util.Objects;

/**
 * Hasil perbandingan.
 *
 * @param changeStarts indeks baris di {@link #rows()} tempat tiap blok perubahan dimulai (untuk navigasi F7)
 */
public record DiffResult(List<DiffRow> rows, DiffStats stats, List<Integer> changeStarts, UnifiedDiff unified) {

    public DiffResult {
        rows = List.copyOf(rows);
        Objects.requireNonNull(stats, "stats");
        changeStarts = List.copyOf(changeStarts);
        Objects.requireNonNull(unified, "unified");
    }
}
