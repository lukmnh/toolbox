package io.github.lukmnh.toolbox.common.task;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.concurrent.Task;

/** Handle task yang sedang berjalan, untuk binding progress dan tombol Batal di UI. */
public final class RunningTask {

    private final Task<?> task;

    RunningTask(Task<?> task) {
        this.task = task;
    }

    /** -1 berarti indeterminate. */
    public ReadOnlyDoubleProperty progressProperty() {
        return task.progressProperty();
    }

    public ReadOnlyBooleanProperty runningProperty() {
        return task.runningProperty();
    }

    public void cancel() {
        task.cancel(true);
    }
}
