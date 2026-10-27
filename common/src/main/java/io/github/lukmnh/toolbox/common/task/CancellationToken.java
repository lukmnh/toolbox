package io.github.lukmnh.toolbox.common.task;

import java.util.concurrent.atomic.AtomicBoolean;

/** Sinyal pembatalan kooperatif. Pekerjaan panjang wajib memanggil {@link #throwIfCancelled()}. */
public final class CancellationToken {

    private final AtomicBoolean cancelled = new AtomicBoolean();

    public static CancellationToken none() {
        return new CancellationToken();
    }

    public void cancel() {
        cancelled.set(true);
    }

    public boolean isCancelled() {
        return cancelled.get() || Thread.currentThread().isInterrupted();
    }

    public void throwIfCancelled() {
        if (isCancelled()) {
            throw new TaskCancelledException();
        }
    }
}
