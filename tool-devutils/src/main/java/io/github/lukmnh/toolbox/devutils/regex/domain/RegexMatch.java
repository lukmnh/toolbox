package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.List;
import java.util.Objects;

/** Satu match: posisi, nilai, dan grup capture urut nomor (tanpa grup 0). */
public record RegexMatch(int start, int end, String value, List<GroupMatch> groups) {

    public RegexMatch {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Posisi match tidak valid");
        }
        Objects.requireNonNull(value, "value");
        groups = List.copyOf(groups);
    }
}
