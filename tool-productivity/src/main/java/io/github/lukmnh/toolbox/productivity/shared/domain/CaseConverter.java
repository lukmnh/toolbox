package io.github.lukmnh.toolbox.productivity.shared.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Konversi huruf untuk Text Tools dan Batch Rename. Semua operasi memakai {@link Locale#ROOT} supaya hasil identifier
 * tidak tergantung locale OS (contoh huruf {@code i} di locale Turki).
 */
public final class CaseConverter {

    /** Kata tugas bahasa Indonesia yang tetap huruf kecil di Title Case gaya Indonesia (kecuali kata pertama). */
    static final Set<String> INDONESIAN_FUNCTION_WORDS = Set.of(
            "di",
            "ke",
            "dari",
            "dan",
            "yang",
            "untuk",
            "pada",
            "dengan",
            "atau",
            "dalam",
            "oleh",
            "bagi",
            "tentang",
            "serta",
            "sebagai",
            "kepada",
            "daripada",
            "pun",
            "per",
            "si",
            "sang");

    private CaseConverter() {}

    /** @param indonesianTitle hanya berpengaruh untuk {@link CaseStyle#TITLE} */
    public static String convert(String text, CaseStyle style, boolean indonesianTitle) {
        return switch (style) {
            case UPPER -> text.toUpperCase(Locale.ROOT);
            case LOWER -> text.toLowerCase(Locale.ROOT);
            case SENTENCE -> sentenceCase(text);
            case TITLE -> titleCase(text, indonesianTitle);
            case CAMEL -> camel(words(text), false);
            case PASCAL -> camel(words(text), true);
            case SNAKE -> join(words(text), "_", false);
            case KEBAB -> join(words(text), "-", false);
            case CONSTANT -> join(words(text), "_", true);
            case DOT -> join(words(text), ".", false);
        };
    }

    /**
     * Pecah teks campuran jadi kata: pemisah = karakter non huruf/angka, plus batas camelCase.
     * {@code HTTPServer_url-path} → {@code [HTTP, Server, url, path]}.
     */
    public static List<String> words(String text) {
        List<String> words = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int[] codePoints = text.codePoints().toArray();
        for (int index = 0; index < codePoints.length; index++) {
            int codePoint = codePoints[index];
            if (!Character.isLetterOrDigit(codePoint)) {
                flush(current, words);
                continue;
            }
            if (startsNewWord(codePoints, index) && !current.isEmpty()) {
                flush(current, words);
            }
            current.appendCodePoint(codePoint);
        }
        flush(current, words);
        return List.copyOf(words);
    }

    /** {@code aB} (huruf kecil → besar) atau {@code ABc} (akhir akronim sebelum kata baru). */
    private static boolean startsNewWord(int[] codePoints, int index) {
        if (index == 0 || !Character.isUpperCase(codePoints[index])) {
            return false;
        }
        int previous = codePoints[index - 1];
        if (Character.isLowerCase(previous) || Character.isDigit(previous)) {
            return true;
        }
        boolean nextIsLower = index + 1 < codePoints.length && Character.isLowerCase(codePoints[index + 1]);
        return Character.isUpperCase(previous) && nextIsLower;
    }

    private static void flush(StringBuilder current, List<String> words) {
        if (!current.isEmpty()) {
            words.add(current.toString());
            current.setLength(0);
        }
    }

    private static String camel(List<String> words, boolean upperFirst) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < words.size(); index++) {
            String word = words.get(index).toLowerCase(Locale.ROOT);
            result.append(index == 0 && !upperFirst ? word : capitalize(word));
        }
        return result.toString();
    }

    private static String join(List<String> words, String separator, boolean upper) {
        return words.stream()
                .map(word -> upper ? word.toUpperCase(Locale.ROOT) : word.toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(separator));
    }

    /** Huruf kecil semua, lalu huruf pertama tiap kalimat (awal teks, setelah {@code . ! ?} + spasi) kapital. */
    private static String sentenceCase(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(lower.length());
        boolean capitalizeNext = true;
        int index = 0;
        while (index < lower.length()) {
            int codePoint = lower.codePointAt(index);
            if (capitalizeNext && Character.isLetter(codePoint)) {
                result.appendCodePoint(Character.toUpperCase(codePoint));
                capitalizeNext = false;
            } else {
                result.appendCodePoint(codePoint);
            }
            if (codePoint == '.' || codePoint == '!' || codePoint == '?' || codePoint == '\n') {
                capitalizeNext = true;
            } else if (Character.isLetterOrDigit(codePoint)) {
                capitalizeNext = false;
            }
            index += Character.charCount(codePoint);
        }
        return result.toString();
    }

    /** Spasi asli dipertahankan; kata pertama tiap baris selalu kapital. */
    private static String titleCase(String text, boolean indonesian) {
        StringBuilder result = new StringBuilder(text.length());
        StringBuilder word = new StringBuilder();
        boolean firstInLine = true;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (!Character.isWhitespace(character)) {
                word.append(character);
                continue;
            }
            firstInLine = appendTitleWord(result, word, firstInLine, indonesian);
            if (character == '\n') {
                firstInLine = true;
            }
            result.append(character);
        }
        appendTitleWord(result, word, firstInLine, indonesian);
        return result.toString();
    }

    /** @return status "kata pertama di baris" setelah kata ini */
    private static boolean appendTitleWord(
            StringBuilder result, StringBuilder word, boolean firstInLine, boolean indonesian) {
        if (word.isEmpty()) {
            return firstInLine;
        }
        String lower = word.toString().toLowerCase(Locale.ROOT);
        boolean keepLower = indonesian && !firstInLine && INDONESIAN_FUNCTION_WORDS.contains(lower);
        result.append(keepLower ? lower : capitalize(lower));
        word.setLength(0);
        return false;
    }

    private static String capitalize(String word) {
        if (word.isEmpty()) {
            return word;
        }
        int first = word.codePointAt(0);
        return new StringBuilder()
                .appendCodePoint(Character.toTitleCase(first))
                .append(word.substring(Character.charCount(first)))
                .toString();
    }
}
