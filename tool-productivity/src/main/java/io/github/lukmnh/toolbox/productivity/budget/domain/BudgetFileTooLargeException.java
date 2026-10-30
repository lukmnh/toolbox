package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.io.Serial;

/** File JSON melebihi batas ukuran; ditolak sebelum dibaca (Security Requirements #3, #19). */
public final class BudgetFileTooLargeException extends BudgetException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;
    private final long sizeBytes;
    private final long maxBytes;

    public BudgetFileTooLargeException(String fileName, long sizeBytes, long maxBytes) {
        super("File budget terlalu besar: " + fileName, null);
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
