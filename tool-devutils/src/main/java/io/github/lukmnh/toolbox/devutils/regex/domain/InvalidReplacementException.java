package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.io.Serial;

/** Template replacement tidak valid (misalnya referensi grup yang tidak ada di flavor Java). */
public final class InvalidReplacementException extends RegexToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int position;
    private final String reference;

    /** @param reference teks referensi yang bermasalah, misalnya {@code } */
    public InvalidReplacementException(int position, String reference) {
        super("Referensi replacement tidak valid: " + reference, null);
        this.position = position;
        this.reference = reference;
    }

    public int position() {
        return position;
    }

    public String reference() {
        return reference;
    }
}
