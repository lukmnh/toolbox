package io.github.lukmnh.toolbox.pdf.shared.domain;

import java.io.Serial;

/** Input range halaman tidak valid. {@link #reason()} menentukan pesan untuk user. */
public final class InvalidPageRangeException extends PdfToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum Reason {
        EMPTY,
        SYNTAX,
        ZERO_OR_NEGATIVE,
        REVERSED,
        OUT_OF_BOUNDS
    }

    private final Reason reason;
    private final String token;
    private final int pageCount;

    public InvalidPageRangeException(Reason reason, String token, int pageCount) {
        super("Invalid range (" + reason + "): '" + token + "' for " + pageCount + " pages", null);
        this.reason = reason;
        this.token = token;
        this.pageCount = pageCount;
    }

    public Reason reason() {
        return reason;
    }

    /** Bagian input yang bermasalah, misalnya {@code "8-3"}. */
    public String token() {
        return token;
    }

    public int pageCount() {
        return pageCount;
    }
}
