package io.github.lukmnh.toolbox.devutils.diff.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Kunci pembanding baris/kata sesuai {@link DiffOptions}. Dua baris dianggap sama bila kuncinya sama. */
public final class LineKey {

    /**
     * Ditempel ke baris terakhir yang tidak diakhiri newline, supaya beda "newline di akhir file" tetap terdeteksi
     * sebagai perubahan (sama seperti {@code git diff}). Karakter NUL tidak mungkin ada di file teks yang lolos
     * {@link BinaryDetector}.
     */
    private static final String NO_NEWLINE_MARKER = "\u0000no-newline";

    private LineKey() {}

    public static String of(String text, DiffOptions options) {
        String key = text;
        if (options.normalizeLineEndings() && key.endsWith("\r")) {
            key = key.substring(0, key.length() - 1);
        }
        key = switch (options.whitespace()) {
            case EXACT -> key;
            case IGNORE_ALL -> withoutWhitespace(key);
            case IGNORE_TRAILING -> key.stripTrailing();
        };
        return options.ignoreCase() ? key.toLowerCase(Locale.ROOT) : key;
    }

    /** Kunci semua baris; dihitung sekali supaya algoritma diff cukup memakai {@code equals}. */
    public static List<String> ofLines(TextLines text, DiffOptions options) {
        List<String> keys = new ArrayList<>(text.size());
        for (int index = 0; index < text.size(); index++) {
            String key = of(text.line(index), options);
            keys.add(text.isLast(index) && !text.endsWithNewline() ? key + NO_NEWLINE_MARKER : key);
        }
        return keys;
    }

    private static String withoutWhitespace(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        text.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint))
                .forEach(builder::appendCodePoint);
        return builder.toString();
    }
}
