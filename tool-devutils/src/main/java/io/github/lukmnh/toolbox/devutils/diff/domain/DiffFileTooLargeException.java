package io.github.lukmnh.toolbox.devutils.diff.domain;

import java.io.Serial;

/** Kedua sisi dimuat utuh ke memori, jadi ukuran dibatasi (Security Requirements #3). */
public final class DiffFileTooLargeException extends DiffException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long sizeBytes;
    private final long maxBytes;

    public DiffFileTooLargeException(String fileName, long sizeBytes, long maxBytes) {
        super("File too large to compare: " + fileName, fileName, null);
        this.sizeBytes = sizeBytes;
        this.maxBytes = maxBytes;
    }

    public long sizeBytes() {
        return sizeBytes;
    }

    public long maxBytes() {
        return maxBytes;
    }
}
