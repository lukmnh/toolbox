package io.github.lukmnh.toolbox.common.task;

import java.io.Serial;

/** Dilempar saat pekerjaan dihentikan karena user menekan Batal. Bukan error. */
public final class TaskCancelledException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public TaskCancelledException() {
        super("Task cancelled");
    }
}
