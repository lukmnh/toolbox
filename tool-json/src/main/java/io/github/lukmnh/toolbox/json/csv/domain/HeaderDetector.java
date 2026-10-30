package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Heuristik "baris pertama adalah header": semua sel baris 1 terisi, unik, dan bukan angka, lalu baris 2 berisi
 * angka atau baris 1 sama sekali tanpa digit. Syarat kedua menangkap CSV teks murni yang hampir selalu juga punya
 * header (misalnya {@code Nama;Kota}), tanpa salah menganggap baris data seperti {@code Budi;Blok 3} sebagai header.
 */
public final class HeaderDetector {

    /** Angka gaya Indonesia/internasional: {@code 12}, {@code -3,5}, {@code 1.234.567,89}, {@code 1e3}, {@code 10%}. */
    private static final Pattern NUMBER = Pattern.compile("[+-]?\\d[\\d.,]*([eE][+-]?\\d+)?%?");

    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");

    private HeaderDetector() {}

    /** @param firstRows maksimal dua record pertama */
    public static boolean looksLikeHeader(List<List<String>> firstRows) {
        if (firstRows.isEmpty() || !qualifies(firstRows.getFirst())) {
            return false;
        }
        if (firstRows.size() < 2) {
            return true;
        }
        return containsNumber(firstRows.get(1)) || !containsDigit(firstRows.getFirst());
    }

    private static boolean qualifies(List<String> row) {
        Set<String> seen = new HashSet<>();
        for (String cell : row) {
            String value = cell.strip();
            if (value.isEmpty() || isNumber(value) || !seen.add(value.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsNumber(List<String> row) {
        return row.stream().map(String::strip).anyMatch(HeaderDetector::isNumber);
    }

    private static boolean containsDigit(List<String> row) {
        return row.stream().anyMatch(cell -> DIGIT.matcher(cell).matches());
    }

    private static boolean isNumber(String value) {
        return NUMBER.matcher(value).matches();
    }
}
