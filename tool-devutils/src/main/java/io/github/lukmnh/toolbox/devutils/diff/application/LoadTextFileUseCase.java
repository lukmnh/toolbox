package io.github.lukmnh.toolbox.devutils.diff.application;

import io.github.lukmnh.toolbox.devutils.diff.domain.LoadedText;
import io.github.lukmnh.toolbox.devutils.diff.domain.TextFiles;
import java.nio.file.Path;

/** Muat file teks untuk salah satu sisi diff (maks 20 MB, file biner ditolak). */
public final class LoadTextFileUseCase {

    private final TextFiles files;

    public LoadTextFileUseCase(TextFiles files) {
        this.files = files;
    }

    public LoadedText execute(Path file) {
        return files.read(file);
    }
}
