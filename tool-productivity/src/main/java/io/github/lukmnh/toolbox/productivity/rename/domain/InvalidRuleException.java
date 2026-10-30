package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.io.Serial;

/** Aturan tidak bisa diterapkan (regex/pola tanggal salah, regex terlalu lama). */
public final class InvalidRuleException extends RenameException {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum Problem {
        INVALID_REGEX,
        INVALID_REPLACEMENT,
        REGEX_TIMEOUT,
        INVALID_DATE_PATTERN
    }

    private final Problem problem;

    public InvalidRuleException(Problem problem, Throwable cause) {
        super("Aturan tidak valid: " + problem, cause);
        this.problem = problem;
    }

    public Problem problem() {
        return problem;
    }
}
