package io.github.lukmnh.toolbox.devutils.sql.application;

import io.github.lukmnh.toolbox.devutils.sql.domain.SqlFiles;
import java.nio.file.Path;

/** Tulis hasil Format/Minify ke file (temp) untuk di-download. */
public final class ExportSqlUseCase {

    private final SqlFiles files;

    public ExportSqlUseCase(SqlFiles files) {
        this.files = files;
    }

    public void execute(String sql, Path target) {
        if (sql.isEmpty()) {
            throw new IllegalArgumentException("No result to export");
        }
        files.write(target, sql);
    }
}
