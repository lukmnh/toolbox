package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Rename yang akan dieksekusi: hanya file yang berubah, semuanya langsung di {@code folder}. */
public record RenamePlan(Path folder, List<RenamePair> pairs) {

    public RenamePlan {
        Objects.requireNonNull(folder, "folder");
        pairs = List.copyOf(pairs);
    }
}
