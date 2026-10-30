package io.github.lukmnh.toolbox.devutils.regex.application;

import io.github.lukmnh.toolbox.devutils.regex.domain.Explanation;
import io.github.lukmnh.toolbox.devutils.regex.domain.ParseResult;
import java.util.List;
import java.util.Objects;

/** Hasil analisis pola tanpa teks uji: pohon, masalah, dan penjelasan. */
public record RegexAnalysis(ParseResult parsed, List<Explanation> explanation) {

    public RegexAnalysis {
        Objects.requireNonNull(parsed, "parsed");
        explanation = List.copyOf(explanation);
    }
}
