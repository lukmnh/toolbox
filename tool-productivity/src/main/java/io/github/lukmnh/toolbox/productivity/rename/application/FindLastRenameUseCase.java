package io.github.lukmnh.toolbox.productivity.rename.application;

import io.github.lukmnh.toolbox.productivity.rename.domain.CorruptHistoryException;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameHistory;
import io.github.lukmnh.toolbox.productivity.rename.domain.StoredRename;
import io.github.lukmnh.toolbox.productivity.rename.domain.UndoUnavailableException;
import java.util.Optional;

/** Ambil log rename terakhir untuk tombol Undo. */
public final class FindLastRenameUseCase {

    private final RenameHistory history;

    public FindLastRenameUseCase(RenameHistory history) {
        this.history = history;
    }

    /**
     * Log rusak tidak bisa dipakai undo dan akan terus menghalangi log yang lebih lama, jadi dihapus lalu dilaporkan.
     *
     * @throws UndoUnavailableException dengan alasan {@code CORRUPT_HISTORY} bila log terakhir rusak
     */
    public Optional<StoredRename> execute() {
        try {
            return history.latest();
        } catch (CorruptHistoryException e) {
            history.delete(e.id());
            throw new UndoUnavailableException(UndoUnavailableException.Reason.CORRUPT_HISTORY, "");
        }
    }
}
