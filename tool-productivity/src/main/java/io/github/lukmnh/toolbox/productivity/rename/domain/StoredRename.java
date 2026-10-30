package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.util.Objects;

/** {@link RenameLog} yang sudah tersimpan, dengan id untuk menghapusnya setelah undo. */
public record StoredRename(String id, RenameLog log) {

    public StoredRename {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(log, "log");
    }
}
