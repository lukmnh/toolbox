package io.github.lukmnh.toolbox.json.viewer.presentation;

import io.github.lukmnh.toolbox.json.viewer.application.ExportViewUseCase;
import io.github.lukmnh.toolbox.json.viewer.application.FilterRecordsUseCase;
import io.github.lukmnh.toolbox.json.viewer.application.OpenJsonFileUseCase;
import io.github.lukmnh.toolbox.json.viewer.application.RecentJsonFilesUseCase;
import io.github.lukmnh.toolbox.json.viewer.application.SortRecordsUseCase;

/** Kumpulan use case JSON Viewer, supaya constructor tool dan ViewModel tidak terlalu panjang. */
public record ViewerUseCases(
        OpenJsonFileUseCase open,
        FilterRecordsUseCase filter,
        SortRecordsUseCase sort,
        ExportViewUseCase export,
        RecentJsonFilesUseCase recent) {}
