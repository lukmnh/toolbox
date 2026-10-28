package io.github.lukmnh.toolbox.devutils.diff.presentation;

import io.github.lukmnh.toolbox.devutils.diff.application.CompareTextsUseCase;
import io.github.lukmnh.toolbox.devutils.diff.application.ExportUnifiedDiffUseCase;
import io.github.lukmnh.toolbox.devutils.diff.application.LoadTextFileUseCase;
import java.util.Objects;

/** Kumpulan use case Text Diff, dirakit di composition root. */
public record DiffUseCases(LoadTextFileUseCase load, CompareTextsUseCase compare, ExportUnifiedDiffUseCase export) {

    public DiffUseCases {
        Objects.requireNonNull(load, "load");
        Objects.requireNonNull(compare, "compare");
        Objects.requireNonNull(export, "export");
    }
}
