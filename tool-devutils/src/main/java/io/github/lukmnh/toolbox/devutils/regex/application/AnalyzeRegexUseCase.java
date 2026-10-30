package io.github.lukmnh.toolbox.devutils.regex.application;

import io.github.lukmnh.toolbox.devutils.regex.domain.ParseResult;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexExplainer;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexParser;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexRequest;

/** Parse pola sesuai flavor lalu susun penjelasan per token (untuk panel Explainer). */
public final class AnalyzeRegexUseCase {

    public RegexAnalysis execute(RegexRequest request) {
        ParseResult parsed = RegexParser.parse(request.pattern(), request.flavor(), request.flags());
        return new RegexAnalysis(parsed, RegexExplainer.explain(parsed, request.flags()));
    }
}
