package io.github.lukmnh.toolbox.json.csv.domain;

/**
 * Satu langkah pipeline kolom. Kolom dirujuk lewat nama (bukan indeks) supaya aturan tetap benar saat urutan langkah
 * diubah. Validasi terhadap kolom yang tersedia dilakukan {@link ColumnPipeline#compile}, karena record ini dibuat
 * terus-menerus saat user mengetik.
 */
public sealed interface PipelineStep permits SplitStep, MergeStep, ArrangeStep {

    boolean enabled();

    PipelineStep withEnabled(boolean enabled);
}
