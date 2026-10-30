package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.io.Serial;

/** Batch melebihi batas jumlah file per eksekusi. */
public final class TooManyFilesException extends RenameException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int maxFiles;

    public TooManyFilesException(int maxFiles) {
        super("File count exceeds the limit of " + maxFiles, null);
        this.maxFiles = maxFiles;
    }

    public int maxFiles() {
        return maxFiles;
    }
}
