package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.io.Serial;

/**
 * File budget tidak sesuai skema. Pesan hanya memuat nama file dan lokasi field (bukan isi), sehingga aman
 * dicatat ke log (Security Requirements #5).
 */
public final class InvalidBudgetFileException extends BudgetException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Jenis pelanggaran skema. */
    public enum Problem {
        MALFORMED_JSON,
        UNSUPPORTED_VERSION,
        MISSING_VALUE,
        INVALID_VALUE,
        NEGATIVE_NUMBER,
        /** File .xlsx/.pdf valid tetapi tidak membawa data budget dari export Toolbox. */
        NOT_TOOLBOX_EXPORT,
        /** File rusak atau bukan .xlsx/.pdf. */
        UNREADABLE_FILE
    }

    private final String fileName;
    private final Problem problem;
    private final String location;

    /** @param location jalur field, misalnya {@code sheets[0].items[3].price}; kosong bila tidak diketahui */
    public InvalidBudgetFileException(String fileName, Problem problem, String location, Throwable cause) {
        super("Invalid budget file: " + fileName + " (" + problem + " " + location + ")", cause);
        this.fileName = fileName;
        this.problem = problem;
        this.location = location;
    }

    public String fileName() {
        return fileName;
    }

    public Problem problem() {
        return problem;
    }

    public String location() {
        return location;
    }
}
