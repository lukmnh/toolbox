package io.github.lukmnh.toolbox.devutils.encoder.domain;

import java.io.Serial;

/** Input Base64 tidak valid. Posisi 0-based terhadap input asli (sebelum whitespace/prefix dibuang). */
public final class InvalidBase64Exception extends EncoderException {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum Reason {
        /** Karakter di luar alfabet Base64 (standar maupun URL-safe). */
        INVALID_CHARACTER,
        /** Ada data setelah padding {@code =}, atau padding lebih dari dua. */
        MISPLACED_PADDING,
        /** Jumlah karakter data tidak membentuk byte utuh (sisa 1 karakter). */
        TRUNCATED
    }

    private final Reason reason;
    private final long position;
    private final int character;

    public InvalidBase64Exception(Reason reason, long position, int character) {
        super("Base64 tidak valid (" + reason + ") di posisi " + position, null);
        this.reason = reason;
        this.position = position;
        this.character = character;
    }

    public Reason reason() {
        return reason;
    }

    public long position() {
        return position;
    }

    /** Karakter penyebab error, atau {@code -1} untuk {@link Reason#TRUNCATED}. */
    public int character() {
        return character;
    }
}
