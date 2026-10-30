package io.github.lukmnh.toolbox.productivity.rename.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.productivity.rename.domain.NameCheck;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameChecker;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameFailedException;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameFileSystem;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameHistory;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameIoException;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenamePair;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameStatus;
import io.github.lukmnh.toolbox.productivity.rename.domain.StoredRename;
import io.github.lukmnh.toolbox.productivity.rename.domain.UndoUnavailableException;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** Balik rename terakhir (juga two-phase + rollback), lalu hapus entry log-nya. */
public final class UndoLastRenameUseCase {

    private static final System.Logger LOG = System.getLogger(UndoLastRenameUseCase.class.getName());

    private final FindLastRenameUseCase findLast;
    private final RenameHistory history;
    private final RenameFileSystem fileSystem;
    private final TwoPhaseRenamer renamer;

    public UndoLastRenameUseCase(
            FindLastRenameUseCase findLast,
            RenameHistory history,
            RenameFileSystem fileSystem,
            TwoPhaseRenamer renamer) {
        this.findLast = findLast;
        this.history = history;
        this.fileSystem = fileSystem;
        this.renamer = renamer;
    }

    /**
     * @throws UndoUnavailableException bila tidak ada log, file hasil rename sudah tidak ada, atau nama asal sudah
     *     dipakai file lain; tidak ada file yang disentuh
     * @throws RenameFailedException bila gagal di tengah (sudah di-rollback)
     */
    public UndoResult execute(ProgressReporter progress, CancellationToken cancellation) {
        StoredRename last = findLast.execute()
                .orElseThrow(() -> new UndoUnavailableException(UndoUnavailableException.Reason.NO_HISTORY, ""));
        Path folder = last.log().folder();
        List<RenamePair> reverse =
                last.log().pairs().stream().map(RenamePair::reversed).toList();
        verifyUndoable(folder, reverse);
        renamer.rename(folder, reverse, progress, cancellation);
        LOG.log(Level.INFO, "Undo batch rename selesai: {0} file", reverse.size());
        return new UndoResult(last, removeLog(last.id()));
    }

    private void verifyUndoable(Path folder, List<RenamePair> reverse) {
        if (!fileSystem.isDirectory(folder)) {
            throw new UndoUnavailableException(UndoUnavailableException.Reason.FOLDER_MISSING, "");
        }
        Set<String> folderNames = fileSystem.entryNames(folder);
        reverse.stream()
                .filter(pair -> !folderNames.contains(pair.from()))
                .findFirst()
                .ifPresent(pair -> {
                    throw new UndoUnavailableException(UndoUnavailableException.Reason.FILE_MISSING, pair.from());
                });
        List<NameCheck> checks = RenameChecker.check(
                reverse.stream().map(RenamePair::from).toList(),
                reverse.stream().map(RenamePair::to).toList(),
                folderNames,
                fileSystem.caseInsensitiveNames());
        for (int index = 0; index < checks.size(); index++) {
            RenameStatus status = checks.get(index).status();
            if (status == RenameStatus.CONFLICT) {
                throw new UndoUnavailableException(
                        UndoUnavailableException.Reason.NAME_TAKEN,
                        reverse.get(index).to());
            }
            // Nama asal pernah ada di folder ini; nama tidak valid berarti isi log tidak bisa dipercaya
            if (status != RenameStatus.OK) {
                throw new UndoUnavailableException(UndoUnavailableException.Reason.CORRUPT_HISTORY, "");
            }
        }
    }

    /** Undo sudah terjadi; gagal menghapus log hanya dilaporkan, tidak membatalkan undo. */
    private boolean removeLog(String id) {
        try {
            history.delete(id);
            return true;
        } catch (RenameIoException e) {
            LOG.log(Level.WARNING, "Log undo gagal dihapus setelah undo");
            return false;
        }
    }
}
