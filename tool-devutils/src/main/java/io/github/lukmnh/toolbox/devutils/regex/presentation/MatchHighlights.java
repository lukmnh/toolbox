package io.github.lukmnh.toolbox.devutils.regex.presentation;

import io.github.lukmnh.toolbox.devutils.regex.domain.GroupMatch;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexMatch;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Menyusun rentang highlight dari hasil match: latar match penuh, lalu warna grup di atasnya. */
final class MatchHighlights {

    /** Jumlah warna grup di tema ({@code -tb-group-1} .. {@code -tb-group-6}); grup berikutnya berputar. */
    static final int GROUP_COLORS = 6;

    /** Panel highlight hanya menampilkan 200 KB pertama supaya layout TextFlow tetap cepat (spec #12). */
    static final int MAX_CHARS = 200_000;

    private MatchHighlights() {}

    static String groupStyle(int groupNumber) {
        return "regex-hl-group-" + colorIndex(groupNumber);
    }

    static int colorIndex(int groupNumber) {
        return (groupNumber - 1) % GROUP_COLORS + 1;
    }

    /** Potong di batas {@link #MAX_CHARS} tanpa memecah pasangan surrogate (emoji). */
    static String visibleText(String text) {
        if (text.length() <= MAX_CHARS) {
            return text;
        }
        int cut = Character.isHighSurrogate(text.charAt(MAX_CHARS - 1)) ? MAX_CHARS - 1 : MAX_CHARS;
        return text.substring(0, cut);
    }

    /**
     * Grup diurutkan dari awal dan dari yang terpanjang, sehingga grup di dalam (lebih pendek) digambar belakangan dan
     * warnanya menang.
     */
    static List<HighlightRange> of(List<RegexMatch> matches, int visibleLength) {
        List<HighlightRange> ranges = new ArrayList<>();
        List<GroupMatch> groups = new ArrayList<>();
        for (RegexMatch match : matches) {
            if (match.start() >= visibleLength) {
                break;
            }
            ranges.add(new HighlightRange(match.start(), Math.min(match.end(), visibleLength), "regex-hl-match"));
            match.groups().stream().filter(GroupMatch::matched).forEach(groups::add);
        }
        groups.sort(Comparator.comparingInt(GroupMatch::start)
                .thenComparing(Comparator.comparingInt((GroupMatch group) -> group.end() - group.start())
                        .reversed()));
        for (GroupMatch group : groups) {
            if (group.start() < visibleLength) {
                ranges.add(new HighlightRange(
                        group.start(), Math.min(group.end(), visibleLength), groupStyle(group.number())));
            }
        }
        return ranges;
    }
}
