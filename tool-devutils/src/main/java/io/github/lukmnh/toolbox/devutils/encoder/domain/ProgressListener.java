package io.github.lukmnh.toolbox.devutils.encoder.domain;

/** Callback progress dari adapter file. Boleh melempar exception runtime untuk membatalkan proses. */
@FunctionalInterface
public interface ProgressListener {

    void advanced(long doneBytes, long totalBytes);
}
