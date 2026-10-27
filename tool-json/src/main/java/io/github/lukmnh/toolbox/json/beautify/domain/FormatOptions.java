package io.github.lukmnh.toolbox.json.beautify.domain;

import java.util.Objects;

/** @param sortKeys urutkan key object secara alfabetis; hanya untuk input ≤ 50 MB */
public record FormatOptions(BeautifyAction action, Indent indent, boolean sortKeys) {

    public FormatOptions {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(indent, "indent");
    }
}
