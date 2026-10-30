package io.github.lukmnh.toolbox.devutils.sql.application;

import io.github.lukmnh.toolbox.devutils.sql.domain.LoadedSql;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlFiles;
import java.nio.file.Path;

/** Muat file {@code .sql} (maks 10 MB). */
public final class LoadSqlFileUseCase {

    private final SqlFiles files;

    public LoadSqlFileUseCase(SqlFiles files) {
        this.files = files;
    }

    public LoadedSql execute(Path file) {
        return files.read(file);
    }
}
