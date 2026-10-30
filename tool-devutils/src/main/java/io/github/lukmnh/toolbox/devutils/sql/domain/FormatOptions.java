package io.github.lukmnh.toolbox.devutils.sql.domain;

import java.util.Objects;

/**
 * Opsi Format SQL.
 *
 * @param uppercaseKeywords {@code false} = huruf keyword dipertahankan seperti input
 * @param blankLinesBetweenQueries baris kosong di antara dua statement
 */
public record FormatOptions(
        SqlDialect dialect, IndentStyle indent, boolean uppercaseKeywords, int blankLinesBetweenQueries) {

    public static final int MAX_BLANK_LINES = 3;

    public FormatOptions {
        Objects.requireNonNull(dialect, "dialect");
        Objects.requireNonNull(indent, "indent");
        if (blankLinesBetweenQueries < 0 || blankLinesBetweenQueries > MAX_BLANK_LINES) {
            throw new IllegalArgumentException("Blank lines between queries must be 0–" + MAX_BLANK_LINES);
        }
    }
}
