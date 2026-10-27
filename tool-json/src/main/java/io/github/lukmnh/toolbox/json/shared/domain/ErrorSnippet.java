package io.github.lukmnh.toolbox.json.shared.domain;

import java.nio.charset.StandardCharsets;

/**
 * Potongan teks di sekitar posisi error, dengan {@link #marker()} = index karakter yang ditunjuk. Tab dan
 * newline diganti spasi supaya penanda {@code ^} di baris berikutnya tetap sejajar.
 */
public record ErrorSnippet(String text, int marker) {

    public static final int DEFAULT_RADIUS = 40;
    public static final ErrorSnippet NONE = new ErrorSnippet("", 0);

    public ErrorSnippet {
        if (marker < 0 || marker > text.length()) {
            throw new IllegalArgumentException("marker di luar teks: " + marker);
        }
    }

    /** Potongan ±{@code radius} karakter di sekitar {@code position} (dijepit ke batas teks). */
    public static ErrorSnippet around(CharSequence text, long position, int radius) {
        int center = (int) Math.clamp(position, 0, text.length());
        int start = Math.max(0, center - radius);
        int end = Math.min(text.length(), center + radius);
        return new ErrorSnippet(flatten(text.subSequence(start, end)), center - start);
    }

    /**
     * Seperti {@link #around}, untuk jendela byte UTF-8 yang dibaca dari file. Byte lanjutan UTF-8 di awal
     * jendela (karakter yang terpotong) dilewati.
     *
     * @param markerByte posisi error di dalam {@code window}
     */
    public static ErrorSnippet aroundUtf8(byte[] window, int markerByte, int radius) {
        int marker = Math.clamp(markerByte, 0, window.length);
        int start = 0;
        while (start < marker && Utf8Text.isContinuationByte(window[start])) {
            start++;
        }
        String before = new String(window, start, marker - start, StandardCharsets.UTF_8);
        String all = new String(window, start, window.length - start, StandardCharsets.UTF_8);
        return around(all, before.length(), radius);
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }

    private static String flatten(CharSequence text) {
        StringBuilder flat = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            flat.append(Character.isWhitespace(c) ? ' ' : c);
        }
        return flat.toString();
    }
}
