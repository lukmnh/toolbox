package io.github.lukmnh.toolbox.devutils.sql.presentation;

import io.github.lukmnh.toolbox.devutils.sql.application.ExportSqlUseCase;
import io.github.lukmnh.toolbox.devutils.sql.application.FormatSqlUseCase;
import io.github.lukmnh.toolbox.devutils.sql.application.LoadSqlFileUseCase;
import io.github.lukmnh.toolbox.devutils.sql.application.MinifySqlUseCase;
import java.util.Objects;

/** Kumpulan use case SQL Formatter, dirakit di composition root. */
public record SqlUseCases(
        LoadSqlFileUseCase load, FormatSqlUseCase format, MinifySqlUseCase minify, ExportSqlUseCase export) {

    public SqlUseCases {
        Objects.requireNonNull(load, "load");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(minify, "minify");
        Objects.requireNonNull(export, "export");
    }
}
