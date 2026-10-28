package io.github.lukmnh.toolbox.devutils.encoder.domain;

import java.util.Locale;

/** Prefix data URI ({@code data:<mime>;base64,}) yang dibuang otomatis saat decode. */
public final class DataUri {

    private static final String SCHEME = "data:";
    private static final String BASE64_MARKER = ";base64,";

    private DataUri() {}

    /** Indeks awal payload Base64 di {@code input}: setelah koma data URI, atau 0 bila bukan data URI. */
    public static int payloadStart(CharSequence input) {
        int start = 0;
        while (start < input.length() && Base64Alphabet.isWhitespace(input.charAt(start))) {
            start++;
        }
        if (!startsWithScheme(input, start)) {
            return 0;
        }
        for (int index = start + SCHEME.length(); index < input.length(); index++) {
            if (input.charAt(index) == ',') {
                return index + 1;
            }
        }
        return 0;
    }

    public static String prefix(String mimeType) {
        return SCHEME + mimeType + BASE64_MARKER;
    }

    private static boolean startsWithScheme(CharSequence input, int start) {
        if (input.length() - start < SCHEME.length()) {
            return false;
        }
        String head = input.subSequence(start, start + SCHEME.length()).toString();
        return head.toLowerCase(Locale.ROOT).equals(SCHEME);
    }
}
