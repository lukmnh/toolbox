package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.Objects;

/** Hasil pipeline atas sampel, beserta laporan record tidak konsisten di sampel. */
public record PipelinePreview(TablePreview table, MismatchReport mismatches) {

    public PipelinePreview {
        Objects.requireNonNull(table, "table");
        Objects.requireNonNull(mismatches, "mismatches");
    }
}
