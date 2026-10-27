package io.github.lukmnh.toolbox.pdf.shared.domain;

import io.github.lukmnh.toolbox.pdf.shared.domain.InvalidPageRangeException.Reason;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Range halaman inklusif, 1-based: {@code 3-5} = halaman 3, 4, 5. */
public record PageRange(int first, int last) {

    private static final Pattern SINGLE = Pattern.compile("\\d+");
    private static final Pattern SPAN = Pattern.compile("(\\d+)\\s*-\\s*(\\d+)");
    private static final String SEPARATOR = ",";

    public PageRange {
        if (first < 1) {
            throw new IllegalArgumentException("first harus >= 1: " + first);
        }
        if (last < first) {
            throw new IllegalArgumentException("last < first: " + first + "-" + last);
        }
    }

    public static PageRange single(int page) {
        return new PageRange(page, page);
    }

    public int pageCount() {
        return last - first + 1;
    }

    /** Label untuk nama file: {@code hal5} atau {@code hal1-3}. */
    public String label() {
        return first == last ? "hal" + first : "hal" + first + "-" + last;
    }

    /** Satu range per halaman: {@code 1, 2, …, pageCount}. */
    public static List<PageRange> eachPage(int pageCount) {
        List<PageRange> ranges = new ArrayList<>(pageCount);
        for (int page = 1; page <= pageCount; page++) {
            ranges.add(single(page));
        }
        return ranges;
    }

    /**
     * Parse input seperti {@code "1-3, 5, 8-10"}. Urutan dipertahankan; tiap bagian menjadi satu range.
     *
     * @throws InvalidPageRangeException bila input kosong, salah format, atau di luar jumlah halaman
     */
    public static List<PageRange> parseList(String input, int pageCount) {
        if (input == null || input.isBlank()) {
            throw new InvalidPageRangeException(Reason.EMPTY, "", pageCount);
        }
        List<PageRange> ranges = new ArrayList<>();
        for (String rawToken : input.split(SEPARATOR, -1)) {
            ranges.add(parseToken(rawToken.strip(), pageCount));
        }
        return List.copyOf(ranges);
    }

    private static PageRange parseToken(String token, int pageCount) {
        if (SINGLE.matcher(token).matches()) {
            int page = parsePage(token, token, pageCount);
            return validated(page, page, token, pageCount);
        }
        Matcher span = SPAN.matcher(token);
        if (!span.matches()) {
            throw new InvalidPageRangeException(Reason.SYNTAX, token, pageCount);
        }
        int first = parsePage(span.group(1), token, pageCount);
        int last = parsePage(span.group(2), token, pageCount);
        return validated(first, last, token, pageCount);
    }

    private static int parsePage(String digits, String token, int pageCount) {
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            // Hanya terjadi untuk angka di luar batas int
            throw new InvalidPageRangeException(Reason.OUT_OF_BOUNDS, token, pageCount);
        }
    }

    private static PageRange validated(int first, int last, String token, int pageCount) {
        if (first < 1 || last < 1) {
            throw new InvalidPageRangeException(Reason.ZERO_OR_NEGATIVE, token, pageCount);
        }
        if (last < first) {
            throw new InvalidPageRangeException(Reason.REVERSED, token, pageCount);
        }
        if (last > pageCount) {
            throw new InvalidPageRangeException(Reason.OUT_OF_BOUNDS, token, pageCount);
        }
        return new PageRange(first, last);
    }
}
