package io.github.lukmnh.toolbox.devutils.regex.presentation;

import io.github.lukmnh.toolbox.devutils.regex.application.AnalyzeRegexUseCase;
import io.github.lukmnh.toolbox.devutils.regex.application.LoadTestTextUseCase;
import io.github.lukmnh.toolbox.devutils.regex.application.ReplaceRegexUseCase;
import io.github.lukmnh.toolbox.devutils.regex.application.RunRegexUseCase;
import java.util.Objects;

/** Kumpulan use case Regex Tester, dirakit di composition root. */
public record RegexUseCases(
        AnalyzeRegexUseCase analyze, RunRegexUseCase run, ReplaceRegexUseCase replace, LoadTestTextUseCase load) {

    public RegexUseCases {
        Objects.requireNonNull(analyze, "analyze");
        Objects.requireNonNull(run, "run");
        Objects.requireNonNull(replace, "replace");
        Objects.requireNonNull(load, "load");
    }
}
