package io.github.lukmnh.toolbox.json.shared.domain;

import java.io.Serial;

/** Filter query JSON Viewer tidak valid. {@link #position()} adalah index karakter (mulai 0). */
public final class InvalidQueryException extends JsonToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum Reason {
        EMPTY,
        UNEXPECTED_TOKEN,
        EXPECTED_FIELD,
        EXPECTED_OPERATOR,
        EXPECTED_VALUE,
        UNCLOSED_STRING,
        UNCLOSED_PARENTHESIS,
        INVALID_PATH
    }

    private final Reason reason;
    private final int position;
    private final String token;

    public InvalidQueryException(Reason reason, int position, String token) {
        super("Query tidak valid (" + reason + ") di posisi " + position + ": '" + token + "'", null);
        this.reason = reason;
        this.position = position;
        this.token = token;
    }

    public Reason reason() {
        return reason;
    }

    public int position() {
        return position;
    }

    /** Bagian query yang bermasalah; kosong bila di akhir input. */
    public String token() {
        return token;
    }
}
