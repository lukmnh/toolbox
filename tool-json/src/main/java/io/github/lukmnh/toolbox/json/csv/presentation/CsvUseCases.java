package io.github.lukmnh.toolbox.json.csv.presentation;

import io.github.lukmnh.toolbox.json.csv.application.ExportCsvUseCase;
import io.github.lukmnh.toolbox.json.csv.application.LoadSampleUseCase;
import io.github.lukmnh.toolbox.json.csv.application.OpenCsvFileUseCase;
import io.github.lukmnh.toolbox.json.csv.application.PreviewPipelineUseCase;
import java.util.Objects;

/** Kumpulan use case tool CSV, dirakit di composition root. */
public record CsvUseCases(
        OpenCsvFileUseCase open,
        LoadSampleUseCase loadSample,
        PreviewPipelineUseCase preview,
        ExportCsvUseCase export) {

    public CsvUseCases {
        Objects.requireNonNull(open, "open");
        Objects.requireNonNull(loadSample, "loadSample");
        Objects.requireNonNull(preview, "preview");
        Objects.requireNonNull(export, "export");
    }
}
