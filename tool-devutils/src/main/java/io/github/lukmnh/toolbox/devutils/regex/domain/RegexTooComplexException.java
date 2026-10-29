package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.io.Serial;

/**
 * Engine kehabisan stack (misalnya {@code (a|b)*} pada teks sangat panjang di {@code java.util.regex}). Dibedakan dari
 * timeout supaya pesan ke user tepat.
 */
public final class RegexTooComplexException extends RegexToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RegexTooComplexException(Throwable cause) {
        super("Pola terlalu kompleks untuk teks ini", cause);
    }
}
