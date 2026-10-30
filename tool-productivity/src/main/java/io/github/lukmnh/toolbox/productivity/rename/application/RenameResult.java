package io.github.lukmnh.toolbox.productivity.rename.application;

import io.github.lukmnh.toolbox.productivity.rename.domain.RenamePair;
import java.util.List;

/**
 * @param pairs rename yang sudah dijalankan
 * @param historySaved {@code false} bila rename berhasil tapi log undo gagal disimpan
 */
public record RenameResult(List<RenamePair> pairs, boolean historySaved) {

    public RenameResult {
        pairs = List.copyOf(pairs);
    }
}
