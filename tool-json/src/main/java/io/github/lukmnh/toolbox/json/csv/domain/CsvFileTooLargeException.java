package io.github.lukmnh.toolbox.json.csv.domain;

import java.io.Serial;

/** File melebihi batas ukuran (Security Requirements #3). Dicek sebelum file dibaca. */
public final class CsvFileTooLargeException extends CsvToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;
    private final long sizeBytes;
    private final long maxBytes;

    public CsvFileTooLargeException(String fileName, long sizeBytes, long maxBytes) {
        super("CSV file too large: " + fileName, null);
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
