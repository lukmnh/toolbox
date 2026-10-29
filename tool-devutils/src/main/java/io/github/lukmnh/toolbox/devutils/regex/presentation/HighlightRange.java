package io.github.lukmnh.toolbox.devutils.regex.presentation;

import java.util.Objects;

/** Rentang teks {@code [start, end)} yang diberi latar dengan style class tertentu. */
record HighlightRange(int start, int end, String styleClass) {

    HighlightRange {
        Objects.requireNonNull(styleClass, "styleClass");
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Rentang highlight tidak valid");
        }
    }
}
