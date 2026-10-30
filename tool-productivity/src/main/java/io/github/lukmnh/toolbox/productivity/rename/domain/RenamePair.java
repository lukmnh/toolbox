package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.util.Objects;

/** Rename satu file di dalam folder batch: {@code from} → {@code to} (nama file saja, bukan path). */
public record RenamePair(String from, String to) {

    public RenamePair {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.isBlank() || to.isBlank()) {
            throw new IllegalArgumentException("Empty file name");
        }
    }

    public RenamePair reversed() {
        return new RenamePair(to, from);
    }
}
