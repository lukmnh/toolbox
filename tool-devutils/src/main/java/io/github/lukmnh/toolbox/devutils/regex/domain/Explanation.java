package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.List;
import java.util.Objects;

/**
 * Satu baris pohon penjelasan.
 *
 * @param depth kedalaman indentasi (0 = tingkat atas)
 * @param quantity pengulangan yang menempel; {@link Quantity#ONCE} bila tidak ada
 * @param groupNumber nomor grup capture untuk pewarnaan (sama dengan warna highlight match), atau 0
 */
public record Explanation(
        int depth, ExplanationKind kind, List<String> args, Quantity quantity, Span span, int groupNumber) {

    public Explanation {
        Objects.requireNonNull(kind, "kind");
        args = List.copyOf(args);
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(span, "span");
    }
}
