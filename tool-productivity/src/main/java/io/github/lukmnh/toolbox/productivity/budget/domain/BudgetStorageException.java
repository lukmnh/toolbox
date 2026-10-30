package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.io.Serial;

/** Gagal membaca/menulis file budget (disk penuh, permission, file hilang). */
public final class BudgetStorageException extends BudgetException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;

    public BudgetStorageException(String fileName, Throwable cause) {
        super("Could not access budget file " + fileName, cause);
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
