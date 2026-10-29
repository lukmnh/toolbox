package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Hasil parse. Parser toleran: selalu menghasilkan pohon (bagian rusak menjadi {@link RegexNode.Unknown}) plus daftar
 * masalah, sehingga explainer tetap bisa menampilkan sebagian pola yang sedang diketik.
 *
 * @param groups grup capture urut nomor
 */
public record ParseResult(
        String pattern, RegexFlavor flavor, RegexNode root, List<GroupInfo> groups, List<RegexProblem> problems) {

    public ParseResult {
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(flavor, "flavor");
        Objects.requireNonNull(root, "root");
        groups = List.copyOf(groups);
        problems = List.copyOf(problems);
    }

    public boolean hasProblems() {
        return !problems.isEmpty();
    }

    /** Nama grup per nomor; indeks 0 = seluruh match (kosong). */
    public List<String> groupNames() {
        List<String> names = new ArrayList<>(groups.size() + 1);
        names.add("");
        groups.forEach(group -> names.add(group.name()));
        return List.copyOf(names);
    }
}
