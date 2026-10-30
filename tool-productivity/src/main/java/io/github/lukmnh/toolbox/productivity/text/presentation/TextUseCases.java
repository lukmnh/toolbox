package io.github.lukmnh.toolbox.productivity.text.presentation;

import io.github.lukmnh.toolbox.productivity.text.application.AnalyzeTextUseCase;
import io.github.lukmnh.toolbox.productivity.text.application.ExportTextUseCase;
import io.github.lukmnh.toolbox.productivity.text.application.LoadTextFileUseCase;
import io.github.lukmnh.toolbox.productivity.text.application.TransformTextUseCase;
import java.util.Objects;

/** Kumpulan use case Text Tools, dirakit di composition root. */
public record TextUseCases(
        AnalyzeTextUseCase analyze,
        TransformTextUseCase transform,
        LoadTextFileUseCase load,
        ExportTextUseCase export) {

    public TextUseCases {
        Objects.requireNonNull(analyze, "analyze");
        Objects.requireNonNull(transform, "transform");
        Objects.requireNonNull(load, "load");
        Objects.requireNonNull(export, "export");
    }
}
