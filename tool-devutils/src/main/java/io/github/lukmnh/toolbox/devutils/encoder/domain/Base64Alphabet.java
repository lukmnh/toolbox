package io.github.lukmnh.toolbox.devutils.encoder.domain;

/**
 * Aturan karakter Base64 yang dipakai decode teks dan decode streaming, supaya keduanya sama-sama toleran: alfabet
 * standar dan URL-safe diterima, whitespace diabaikan.
 */
public final class Base64Alphabet {

    public static final char PADDING = '=';

    private Base64Alphabet() {}

    /** Karakter alfabet standar padanannya ({@code -} → {@code +}, {@code _} → {@code /}), atau -1 bila invalid. */
    public static int normalize(int character) {
        if ((character >= 'A' && character <= 'Z')
                || (character >= 'a' && character <= 'z')
                || (character >= '0' && character <= '9')
                || character == '+'
                || character == '/') {
            return character;
        }
        return switch (character) {
            case '-' -> '+';
            case '_' -> '/';
            default -> -1;
        };
    }

    public static boolean isWhitespace(int character) {
        return character == ' ' || character == '\t' || character == '\n' || character == '\r' || character == '\f';
    }
}
