package io.github.lukmnh.toolbox.devutils.regex.infrastructure;

import io.github.lukmnh.toolbox.devutils.regex.domain.GroupMatch;
import io.github.lukmnh.toolbox.devutils.regex.domain.MatchScan;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexMatch;
import io.github.lukmnh.toolbox.devutils.regex.domain.ScanLimits;
import java.util.ArrayList;
import java.util.List;

/**
 * Mengumpulkan match dari engine mana pun: detail disimpan sampai {@link ScanLimits#maxMatches()}, sisanya hanya
 * dihitung sampai {@link ScanLimits#maxCount()}. Nilai diambil dari teks asli (bukan pembungkus yang diawasi timeout)
 * supaya substring tidak ikut dihitung sebagai pembacaan engine.
 */
final class MatchCollector {

    private final String text;
    private final List<String> groupNames;
    private final int groupCount;
    private final ScanLimits limits;
    private final List<RegexMatch> matches = new ArrayList<>();
    private int count;

    MatchCollector(String text, List<String> groupNames, int groupCount, ScanLimits limits) {
        this.text = text;
        this.groupNames = groupNames;
        this.groupCount = groupCount;
        this.limits = limits;
    }

    /** {@code true} bila batas hitungan sudah tercapai dan pencarian harus berhenti. */
    boolean isFull() {
        return count >= limits.maxCount();
    }

    void add(GroupBounds bounds) {
        if (matches.size() < limits.maxMatches()) {
            matches.add(toMatch(bounds));
        }
        count++;
    }

    MatchScan result(boolean countComplete) {
        return new MatchScan(matches, count, countComplete);
    }

    private RegexMatch toMatch(GroupBounds bounds) {
        int start = bounds.start(0);
        int end = bounds.end(0);
        List<GroupMatch> groups = new ArrayList<>(groupCount);
        for (int group = 1; group <= groupCount; group++) {
            groups.add(groupMatch(group, bounds.start(group), bounds.end(group)));
        }
        return new RegexMatch(start, end, text.substring(start, end), groups);
    }

    private GroupMatch groupMatch(int number, int start, int end) {
        String name = number < groupNames.size() ? groupNames.get(number) : "";
        if (start < 0 || end < 0) {
            return new GroupMatch(number, name, GroupMatch.NOT_MATCHED, GroupMatch.NOT_MATCHED, "");
        }
        return new GroupMatch(number, name, start, end, text.substring(start, end));
    }
}
