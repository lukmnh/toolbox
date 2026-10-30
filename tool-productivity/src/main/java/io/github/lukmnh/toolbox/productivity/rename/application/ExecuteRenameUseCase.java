package io.github.lukmnh.toolbox.productivity.rename.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.productivity.rename.domain.PlanOutdatedException;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameChecker;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameFailedException;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameFileSystem;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameHistory;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameIoException;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameLog;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenamePair;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenamePlan;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameStatus;
import io.github.lukmnh.toolbox.productivity.rename.domain.TooManyFilesException;
import java.lang.System.Logger.Level;
import java.time.Clock;
import java.util.List;
import java.util.Set;

/** Jalankan rename hasil preview (two-phase + rollback), lalu simpan log undo. */
public final class ExecuteRenameUseCase {

    private static final System.Logger LOG = System.getLogger(ExecuteRenameUseCase.class.getName());

    private final RenameFileSystem fileSystem;
    private final TwoPhaseRenamer renamer;
    private final RenameHistory history;
    private final Clock clock;
    private final int maxFiles;

    public ExecuteRenameUseCase(
            RenameFileSystem fileSystem, TwoPhaseRenamer renamer, RenameHistory history, Clock clock, int maxFiles) {
        this.fileSystem = fileSystem;
        this.renamer = renamer;
        this.history = history;
        this.clock = clock;
        this.maxFiles = maxFiles;
    }

    /**
     * @throws PlanOutdatedException bila isi folder berubah sejak preview sehingga rencana tidak lagi aman
     * @throws RenameFailedException bila gagal di tengah (sudah di-rollback)
     */
    public RenameResult execute(RenamePlan plan, ProgressReporter progress, CancellationToken cancellation) {
        if (plan.pairs().size() > maxFiles) {
            throw new TooManyFilesException(maxFiles);
        }
        verifyStillValid(plan);
        renamer.rename(plan.folder(), plan.pairs(), progress, cancellation);
        boolean saved = saveHistory(plan);
        LOG.log(Level.INFO, "Batch rename selesai: {0} file", plan.pairs().size());
        return new RenameResult(plan.pairs(), saved);
    }

    /** Preview dihitung beberapa saat sebelumnya; folder bisa saja sudah berubah sejak itu. */
    private void verifyStillValid(RenamePlan plan) {
        if (plan.pairs().isEmpty()) {
            throw new PlanOutdatedException();
        }
        Set<String> folderNames = fileSystem.entryNames(plan.folder());
        List<String> sources = plan.pairs().stream().map(RenamePair::from).toList();
        List<String> targets = plan.pairs().stream().map(RenamePair::to).toList();
        if (!folderNames.containsAll(sources)) {
            throw new PlanOutdatedException();
        }
        boolean allOk = RenameChecker.check(sources, targets, folderNames, fileSystem.caseInsensitiveNames()).stream()
                .allMatch(check -> check.status() == RenameStatus.OK);
        if (!allOk) {
            throw new PlanOutdatedException();
        }
    }

    /** File sudah ter-rename; gagal menyimpan log tidak boleh membatalkannya, cukup dilaporkan. */
    private boolean saveHistory(RenamePlan plan) {
        try {
            history.save(new RenameLog(plan.folder(), clock.instant(), plan.pairs()));
            return true;
        } catch (RenameIoException e) {
            // Pesan IOException memuat path, jadi hanya tipe error yang dicatat (Security Requirements #5)
            String errorType =
                    e.getCause() == null ? "" : e.getCause().getClass().getSimpleName();
            LOG.log(Level.WARNING, "Log undo gagal disimpan: {0}", errorType);
            return false;
        }
    }
}
