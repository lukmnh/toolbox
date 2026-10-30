package io.github.lukmnh.toolbox.devutils.mock.domain;

import java.io.Serial;

/** Schema tidak valid; {@code subject} = nama kolom/tabel/nilai yang bermasalah, untuk ditampilkan ke user. */
public final class InvalidMockSchemaException extends MockToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum Reason {
        ROWS_OUT_OF_RANGE,
        NO_COLUMNS,
        TOO_MANY_COLUMNS,
        INVALID_TABLE,
        BLANK_NAME,
        INVALID_NAME,
        DUPLICATE_NAME,
        INVALID_RANGE,
        EMPTY_ENUM,
        INVALID_PATTERN,
        INVALID_SEED
    }

    private final Reason reason;
    private final String subject;

    public InvalidMockSchemaException(Reason reason, String subject) {
        super("Invalid mock schema: " + reason, null);
        this.reason = reason;
        this.subject = subject;
    }

    public Reason reason() {
        return reason;
    }

    public String subject() {
        return subject;
    }
}
