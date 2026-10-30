package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.List;

/**
 * @param matches detail match pertama (maks {@link ScanLimits#maxMatches()})
 * @param totalCount jumlah match yang ditemukan
 * @param countComplete {@code false} bila penghitungan berhenti di {@link ScanLimits#maxCount()}
 */
public record MatchScan(List<RegexMatch> matches, int totalCount, boolean countComplete) {

    public MatchScan {
        matches = List.copyOf(matches);
        if (totalCount < matches.size()) {
            throw new IllegalArgumentException("Total matches is smaller than the details");
        }
    }
}
