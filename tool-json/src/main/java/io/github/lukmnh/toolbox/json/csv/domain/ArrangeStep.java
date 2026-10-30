package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.List;

/**
 * Pilih, urutkan ulang, ganti nama, dan hapus kolom. Urutan {@code rules} = urutan hasil. Kolom yang belum punya
 * aturan (misalnya hasil langkah sebelumnya yang baru ditambah) tetap dipertahankan di akhir, supaya mengubah langkah
 * lain tidak diam-diam membuang data.
 */
public record ArrangeStep(boolean enabled, List<ColumnRule> rules) implements PipelineStep {

    public ArrangeStep {
        rules = List.copyOf(rules);
    }

    /** Langkah baru: semua kolom saat ini dipertahankan apa adanya. */
    public static ArrangeStep create(List<String> columns) {
        return new ArrangeStep(true, columns.stream().map(ColumnRule::unchanged).toList());
    }

    @Override
    public ArrangeStep withEnabled(boolean enabled) {
        return new ArrangeStep(enabled, rules);
    }
}
