package io.github.lukmnh.toolbox.devutils.sql.domain;

import java.io.Serial;

/** File dimuat utuh ke memori, jadi ukurannya dibatasi (Security Requirements #3). */
public final class SqlFileTooLargeException extends SqlToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;
    private final long sizeBytes;
    private final long maxBytes;

    public SqlFileTooLargeException(String fileName, long sizeBytes, long maxBytes) {
        super("File SQL terlalu besar: " + fileName, null);
        this.fileName = fileName;
        this.sizeBytes = sizeBytes;
        this.maxBytes = maxBytes;
    }

    public String fileName() {
        return fileName;
    }

    public long sizeBytes() {
        return sizeBytes;
    }

    public long maxBytes() {
        return maxBytes;
    }
}
