package io.github.lukmnh.toolbox.productivity.rename.application;

import io.github.lukmnh.toolbox.productivity.rename.domain.StoredRename;
import java.util.Objects;

/**
 * @param undone log yang sudah dibalik
 * @param logRemoved {@code false} bila undo berhasil tapi entry log gagal dihapus
 */
public record UndoResult(StoredRename undone, boolean logRemoved) {

    public UndoResult {
        Objects.requireNonNull(undone, "undone");
    }
}
