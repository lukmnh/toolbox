package io.github.lukmnh.toolbox.productivity.text.domain;

import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

/** Rapikan teks per baris. Baris baru di akhir teks dipertahankan supaya file tetap diakhiri newline. */
public final class LineTidier {

    private static final String NEW_LINE = "\n";
    private static final String SINGLE_SPACE = " ";
    /** {@code \h} = spasi horizontal termasuk tab dan NBSP. */
    private static final Pattern HORIZONTAL_SPACES = Pattern.compile("\\h+");

    private LineTidier() {}

    public static String apply(String text, TidyOperation operation) {
        if (text.isEmpty()) {
            return text;
        }
        boolean trailingNewLine = text.endsWith(NEW_LINE);
        String body = trailingNewLine ? text.substring(0, text.length() - 1) : text;
        List<String> lines = Arrays.asList(body.split(NEW_LINE, -1));
        String result = String.join(NEW_LINE, transform(lines, operation));
        return trailingNewLine && !result.isEmpty() ? result + NEW_LINE : result;
    }

    private static List<String> transform(List<String> lines, TidyOperation operation) {
        return switch (operation) {
            case COLLAPSE_SPACES ->
                lines.stream()
                        .map(line -> HORIZONTAL_SPACES
                                .matcher(line)
                                .replaceAll(SINGLE_SPACE)
                                .strip())
                        .toList();
            case REMOVE_BLANK_LINES ->
                lines.stream().filter(line -> !line.isBlank()).toList();
            case REMOVE_DUPLICATE_LINES -> List.copyOf(new LinkedHashSet<>(lines));
            case SORT_ASCENDING -> sorted(lines, false);
            case SORT_DESCENDING -> sorted(lines, true);
            case REVERSE_LINES -> reversed(lines);
        };
    }

    /**
     * Urut dengan {@link Collator} Indonesia (huruf besar/kecil & aksen diurutkan wajar, bukan per kode Unicode).
     * {@link CollationKey} dihitung sekali per baris karena jauh lebih cepat daripada {@code Collator.compare} berulang
     * untuk ratusan ribu baris.
     */
    private static List<String> sorted(List<String> lines, boolean descending) {
        Collator collator = Collator.getInstance(TextAnalyzer.INDONESIAN);
        Comparator<CollationKey> order = descending ? Comparator.reverseOrder() : Comparator.naturalOrder();
        return lines.stream()
                .map(collator::getCollationKey)
                .sorted(order)
                .map(CollationKey::getSourceString)
                .toList();
    }

    private static List<String> reversed(List<String> lines) {
        List<String> copy = new ArrayList<>(lines);
        Collections.reverse(copy);
        return copy;
    }
}
