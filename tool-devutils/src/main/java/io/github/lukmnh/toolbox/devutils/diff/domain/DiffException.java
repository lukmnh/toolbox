package io.github.lukmnh.toolbox.devutils.diff.domain;

import java.io.Serial;

/** Semua error domain Text Diff; sealed supaya pemetaan pesan di presentation exhaustive. */
public abstract sealed class DiffException extends RuntimeException
        permits BinaryFileException, DiffFileTooLargeException, DiffIoException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;

    protected DiffException(String message, String fileName, Throwable cause) {
        super(message, cause);
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
