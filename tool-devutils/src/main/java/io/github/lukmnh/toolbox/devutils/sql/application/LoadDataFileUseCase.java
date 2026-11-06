package io.github.lukmnh.toolbox.devutils.sql.application;

import io.github.lukmnh.toolbox.devutils.sql.domain.DataFiles;
import io.github.lukmnh.toolbox.devutils.sql.domain.DataTable;
import java.nio.file.Path;

/** Muat data SQL Generator ({@code .xlsx}, {@code .csv}, {@code .txt}, {@code .json}). */
public final class LoadDataFileUseCase {

    private final DataFiles files;

    public LoadDataFileUseCase(DataFiles files) {
        this.files = files;
    }

    public DataTable execute(Path file) {
        return files.read(file);
    }
}
