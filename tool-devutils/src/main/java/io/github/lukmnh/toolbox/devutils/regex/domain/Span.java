package io.github.lukmnh.toolbox.devutils.regex.domain;

/** Rentang di pola asli: {@code [start, end)} dalam indeks {@code char}. */
public record Span(int start, int end) {

    public Span {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Span tidak valid: " + start + ".." + end);
        }
    }

    public int length() {
        return end - start;
    }
}
