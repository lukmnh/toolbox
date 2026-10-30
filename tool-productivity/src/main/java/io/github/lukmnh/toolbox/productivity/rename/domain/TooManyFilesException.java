package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.io.Serial;

/** Batch melebihi batas jumlah file per eksekusi. */
public final class TooManyFilesException extends RenameException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int maxFiles;

    public TooManyFilesException(int maxFiles) {
        super("Jumlah file melebihi batas " + maxFiles, null);
        this.maxFiles = maxFiles;
    }

    public int maxFiles() {
        return maxFiles;
    }
}
