package io.github.lukmnh.toolbox.common.task;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.concurrent.Task;

/**
 * Menjalankan {@link BackgroundWork} di virtual thread dan mengirim hasilnya kembali ke FX thread.
 *
 * <p>Callback {@code onSuccess} dan {@code onFailure} selalu dipanggil di FX thread. Pembatalan tidak
 * memanggil callback mana pun.
 */
public final class TaskRunner implements AutoCloseable {

    private static final System.Logger LOG = System.getLogger(TaskRunner.class.getName());

    private final ExecutorService executor;

    public TaskRunner() {
        this(Executors.newVirtualThreadPerTaskExecutor());
    }

    TaskRunner(ExecutorService executor) {
        this.executor = Objects.requireNonNull(executor);
    }

    public <T> RunningTask submit(
            BackgroundWork<T> work, Consumer<? super T> onSuccess, Consumer<? super Throwable> onFailure) {
        var task = new CancellableTask<>(work);
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> notifyFailure(task.getException(), onFailure));
        executor.execute(task);
        return new RunningTask(task);
    }

    private static void notifyFailure(Throwable error, Consumer<? super Throwable> onFailure) {
        // Pembatalan yang terdeteksi dari dalam pekerjaan bukan error bagi user
        if (error instanceof TaskCancelledException) {
            return;
        }
        // Hanya nama tipe: pesan exception bisa memuat isi dokumen user (Security Requirements #5)
        LOG.log(Level.WARNING, "Background task gagal: " + error.getClass().getName());
        onFailure.accept(error);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private static final class CancellableTask<T> extends Task<T> {

        private final BackgroundWork<T> work;
        private final CancellationToken token = new CancellationToken();

        private CancellableTask(BackgroundWork<T> work) {
            this.work = work;
        }

        @Override
        protected T call() throws Exception {
            return work.execute(this::updateProgress, token);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            token.cancel();
            return super.cancel(mayInterruptIfRunning);
        }
    }
}
