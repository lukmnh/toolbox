package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.io.Serial;

/**
 * Engine menolak pola. {@code description} = pesan asli engine (Java/RE2), ditampilkan apa adanya supaya sama dengan
 * yang dilihat developer di bahasa tersebut.
 *
 * @param position indeks di pola asli, atau {@link #UNKNOWN_POSITION}
 */
public final class RegexCompileException extends RegexToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int UNKNOWN_POSITION = -1;

    private final String description;
    private final int position;

    public RegexCompileException(String description, int position, Throwable cause) {
        super(description, cause);
        this.description = description;
        this.position = position;
    }

    public String description() {
        return description;
    }

    public int position() {
        return position;
    }
}
