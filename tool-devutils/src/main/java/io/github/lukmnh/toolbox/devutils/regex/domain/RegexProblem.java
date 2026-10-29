package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.List;
import java.util.Objects;

/** Satu masalah di pola beserta posisinya (untuk penanda {@code ^}). */
public record RegexProblem(Span span, ProblemCode code, List<String> args) {

    public RegexProblem {
        Objects.requireNonNull(span, "span");
        Objects.requireNonNull(code, "code");
        args = List.copyOf(args);
    }

    public static RegexProblem of(Span span, ProblemCode code, String... args) {
        return new RegexProblem(span, code, List.of(args));
    }
}
