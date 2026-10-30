package io.github.lukmnh.toolbox.productivity.budget.infrastructure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Nama sheet Excel yang valid: maks 31 karakter, tanpa {@code [ ] : * ? / \}, tidak diawali/diakhiri tanda kutip,
 * dan unik tanpa membedakan huruf besar/kecil (aturan Excel).
 */
final class SheetNames {

    static final int MAX_LENGTH = 31;
    private static final Pattern FORBIDDEN = Pattern.compile("[\\[\\]:*?/\\\\\\p{Cntrl}]");
    private static final Pattern EDGE_QUOTES = Pattern.compile("^['\\s]+|['\\s]+$");
    /** Excel memakai nama ini untuk sheet internal riwayat perubahan. */
    private static final String RESERVED = "history";

    private static final int FIRST_SUFFIX = 2;

    private SheetNames() {}

    /** @return nama final dengan urutan sama seperti {@code desired} */
    static List<String> assign(List<String> desired, String fallback) {
        Set<String> taken = new HashSet<>(Set.of(RESERVED));
        List<String> names = new ArrayList<>(desired.size());
        for (String name : desired) {
            String unique = unique(clean(name, fallback), taken);
            taken.add(unique.toLowerCase(Locale.ROOT));
            names.add(unique);
        }
        return List.copyOf(names);
    }

    private static String clean(String name, String fallback) {
        String cleaned =
                EDGE_QUOTES.matcher(FORBIDDEN.matcher(name).replaceAll("")).replaceAll("");
        return cleaned.isEmpty() ? fallback : truncate(cleaned, MAX_LENGTH);
    }

    private static String unique(String name, Set<String> taken) {
        if (!taken.contains(name.toLowerCase(Locale.ROOT))) {
            return name;
        }
        for (int counter = FIRST_SUFFIX; ; counter++) {
            String suffix = " (" + counter + ")";
            String candidate = truncate(name, MAX_LENGTH - suffix.length()).strip() + suffix;
            if (!taken.contains(candidate.toLowerCase(Locale.ROOT))) {
                return candidate;
            }
        }
    }

    /** Potong per code point supaya emoji tidak terbelah. */
    private static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        int end = maxLength;
        if (Character.isHighSurrogate(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end);
    }
}
