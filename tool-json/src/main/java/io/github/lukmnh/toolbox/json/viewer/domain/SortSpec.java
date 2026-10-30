package io.github.lukmnh.toolbox.json.viewer.domain;

import java.util.Objects;

public record SortSpec(RecordColumn column, boolean ascending) {

    public SortSpec {
        Objects.requireNonNull(column, "column");
    }
}
