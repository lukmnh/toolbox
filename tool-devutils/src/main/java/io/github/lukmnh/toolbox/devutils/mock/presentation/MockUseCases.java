package io.github.lukmnh.toolbox.devutils.mock.presentation;

import io.github.lukmnh.toolbox.devutils.mock.application.ExportMockUseCase;
import io.github.lukmnh.toolbox.devutils.mock.application.PreviewMockUseCase;
import java.util.Objects;

/** Kumpulan use case Mock Data Generator, dirakit di composition root. */
public record MockUseCases(PreviewMockUseCase preview, ExportMockUseCase export) {

    public MockUseCases {
        Objects.requireNonNull(preview, "preview");
        Objects.requireNonNull(export, "export");
    }
}
