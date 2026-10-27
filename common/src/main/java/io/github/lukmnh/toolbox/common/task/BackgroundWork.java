package io.github.lukmnh.toolbox.common.task;

/** Pekerjaan yang dijalankan di background thread oleh {@link TaskRunner}. */
@FunctionalInterface
public interface BackgroundWork<T> {

    T execute(ProgressReporter progress, CancellationToken cancellation) throws Exception;
}
