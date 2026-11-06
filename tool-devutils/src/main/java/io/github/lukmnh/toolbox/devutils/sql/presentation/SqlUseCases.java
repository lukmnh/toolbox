package io.github.lukmnh.toolbox.devutils.sql.presentation;

import io.github.lukmnh.toolbox.devutils.sql.application.ExportSqlUseCase;
import io.github.lukmnh.toolbox.devutils.sql.application.FormatSqlUseCase;
import io.github.lukmnh.toolbox.devutils.sql.application.GenerateSyntaxUseCase;
import io.github.lukmnh.toolbox.devutils.sql.application.LoadDataFileUseCase;
import io.github.lukmnh.toolbox.devutils.sql.application.LoadSqlFileUseCase;
import io.github.lukmnh.toolbox.devutils.sql.application.MinifySqlUseCase;
import java.util.Objects;

/** Kumpulan use case SQL Formatter + Generator, dirakit di composition root. */
public record SqlUseCases(
        LoadSqlFileUseCase load,
        FormatSqlUseCase format,
        MinifySqlUseCase minify,
        ExportSqlUseCase export,
        LoadDataFileUseCase loadData,
        GenerateSyntaxUseCase generate) {

    public SqlUseCases {
        Objects.requireNonNull(load, "load");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(minify, "minify");
        Objects.requireNonNull(export, "export");
        Objects.requireNonNull(loadData, "loadData");
        Objects.requireNonNull(generate, "generate");
    }
}
