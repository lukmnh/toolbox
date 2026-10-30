package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Catatan undo satu eksekusi rename. */
public record RenameLog(Path folder, Instant createdAt, List<RenamePair> pairs) {

    public RenameLog {
        Objects.requireNonNull(folder, "folder");
        Objects.requireNonNull(createdAt, "createdAt");
        pairs = List.copyOf(pairs);
        if (pairs.isEmpty()) {
            throw new IllegalArgumentException("Log undo tanpa file");
        }
    }
}
