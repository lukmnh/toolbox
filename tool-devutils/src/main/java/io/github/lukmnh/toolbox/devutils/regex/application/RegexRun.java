package io.github.lukmnh.toolbox.devutils.regex.application;

import io.github.lukmnh.toolbox.devutils.regex.domain.MatchScan;
import java.util.List;
import java.util.Objects;

/** @param groupNames nama tampilan grup per nomor (indeks 0 = seluruh match) */
public record RegexRun(MatchScan scan, List<String> groupNames) {

    public RegexRun {
        Objects.requireNonNull(scan, "scan");
        groupNames = List.copyOf(groupNames);
    }

    public int groupCount() {
        return groupNames.size() - 1;
    }
}
