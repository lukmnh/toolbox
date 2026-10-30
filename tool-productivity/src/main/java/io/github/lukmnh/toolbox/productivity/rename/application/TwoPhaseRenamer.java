package io.github.lukmnh.toolbox.productivity.rename.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.common.task.TaskCancelledException;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameException;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameFailedException;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameFileSystem;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenamePair;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Rename dua fase dengan rollback otomatis (Security Requirements #21), dipakai eksekusi dan undo.
 *
 * <p>Fase 1 memindahkan semua file ke nama sementara unik di folder yang sama, fase 2 ke nama final. Dengan begitu
 * swap {@code a↔b} dan rename yang hanya beda huruf besar-kecil (Windows) tidak pernah menabrak file lain. Bila satu
 * langkah gagal atau dibatalkan, semua langkah yang sudah jalan dibalik dengan urutan terbalik.
 */
public final class TwoPhaseRenamer {

    private static final System.Logger LOG = System.getLogger(TwoPhaseRenamer.class.getName());
    private static final String TEMP_PREFIX = ".toolbox-rename-";
    private static final String TEMP_SUFFIX = ".tmp";
    private static final int PHASES = 2;

    private final RenameFileSystem fileSystem;

    public TwoPhaseRenamer(RenameFileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    /**
     * @throws RenameFailedException bila satu langkah gagal; file sudah dikembalikan ke nama asal sebisanya
     * @throws TaskCancelledException bila dibatalkan dan semua file berhasil dikembalikan
     */
    public void rename(Path folder, List<RenamePair> pairs, ProgressReporter progress, CancellationToken cancellation) {
        List<Step> steps = plan(pairs);
        Deque<Step> done = new ArrayDeque<>(steps.size());
        for (Step step : steps) {
            if (cancellation.isCancelled()) {
                throw abort(folder, done, step, new TaskCancelledException());
            }
            try {
                fileSystem.move(folder, step.from(), step.to());
            } catch (RenameException e) {
                throw abort(folder, done, step, e);
            }
            done.push(step);
            progress.report(done.size(), steps.size());
        }
    }

    private static List<Step> plan(List<RenamePair> pairs) {
        String batchId = UUID.randomUUID().toString();
        List<Step> steps = new ArrayList<>(pairs.size() * PHASES);
        List<String> temps = new ArrayList<>(pairs.size());
        for (int index = 0; index < pairs.size(); index++) {
            String temp = TEMP_PREFIX + batchId + "-" + index + TEMP_SUFFIX;
            temps.add(temp);
            steps.add(new Step(pairs.get(index).from(), temp, pairs.get(index).from()));
        }
        for (int index = 0; index < pairs.size(); index++) {
            steps.add(new Step(
                    temps.get(index), pairs.get(index).to(), pairs.get(index).from()));
        }
        return steps;
    }

    /** Rollback lalu kembalikan exception yang harus dilempar pemanggil. */
    private RuntimeException abort(Path folder, Deque<Step> done, Step failed, RuntimeException cause) {
        int stepsToUndo = done.size();
        List<String> notRestored = rollback(folder, done);
        // Hanya jumlah yang dicatat, nama file tidak boleh masuk log (Security Requirements #5)
        LOG.log(
                Level.WARNING,
                "Batch rename dihentikan ({0}); rollback {1} langkah, {2} file gagal dikembalikan",
                cause.getClass().getSimpleName(),
                stepsToUndo,
                notRestored.size());
        if (cause instanceof TaskCancelledException && notRestored.isEmpty()) {
            return cause;
        }
        return new RenameFailedException(failed.original(), notRestored, cause);
    }

    /** @return nama asal file yang tidak berhasil dikembalikan */
    private List<String> rollback(Path folder, Deque<Step> done) {
        Set<String> notRestored = new LinkedHashSet<>();
        while (!done.isEmpty()) {
            Step step = done.pop();
            // Langkah sebelumnya untuk file ini sudah gagal dibalik; jangan pindahkan file lain ke nama itu
            if (notRestored.contains(step.original())) {
                continue;
            }
            try {
                fileSystem.move(folder, step.to(), step.from());
            } catch (RenameException e) {
                notRestored.add(step.original());
            }
        }
        return List.copyOf(notRestored);
    }

    /** @param original nama asal file sebelum batch, untuk laporan ke user */
    private record Step(String from, String to, String original) {}
}
