package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Nama kolom input. Langkah pipeline merujuk kolom lewat nama, jadi nama wajib unik dan tidak kosong. File tanpa
 * header memakai huruf kolom ala spreadsheet ({@code A}, {@code B}, …, {@code AA}) yang netral bahasa.
 */
public final class ColumnNames {

    private static final int ALPHABET = 26;
    private static final String DUPLICATE_SEPARATOR = "_";

    private ColumnNames() {}

    /** Nama dari baris header: yang kosong diganti huruf kolom, duplikat diberi akhiran {@code _2}, {@code _3}, …. */
    public static List<String> fromHeader(List<String> header) {
        List<String> names = new ArrayList<>(header.size());
        Set<String> used = new HashSet<>();
        for (int index = 0; index < header.size(); index++) {
            String name = header.get(index).strip();
            String base = name.isEmpty() ? letter(index) : name;
            String unique = base;
            for (int suffix = 2; !used.add(unique); suffix++) {
                unique = base + DUPLICATE_SEPARATOR + suffix;
            }
            names.add(unique);
        }
        return List.copyOf(names);
    }

    /** Nama kolom dari record pertama: header bila ada, selain itu huruf kolom selebar record tersebut. */
    public static List<String> forFirstRecord(List<String> firstRecord, boolean hasHeader) {
        return hasHeader ? fromHeader(firstRecord) : generated(firstRecord.size());
    }

    public static List<String> generated(int count) {
        List<String> names = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            names.add(letter(index));
        }
        return List.copyOf(names);
    }

    /** 0 → A, 25 → Z, 26 → AA. */
    public static String letter(int index) {
        StringBuilder letters = new StringBuilder();
        for (int remaining = index + 1; remaining > 0; remaining = (remaining - 1) / ALPHABET) {
            letters.insert(0, (char) ('A' + (remaining - 1) % ALPHABET));
        }
        return letters.toString();
    }
}
