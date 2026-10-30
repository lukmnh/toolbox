package io.github.lukmnh.toolbox.productivity.text.application;

import io.github.lukmnh.toolbox.productivity.text.domain.LoadedText;
import io.github.lukmnh.toolbox.productivity.text.domain.TextFiles;
import java.nio.file.Path;

/** Muat file {@code .txt} (maks batas Text Tools) ke editor. */
public final class LoadTextFileUseCase {

    private final TextFiles files;

    public LoadTextFileUseCase(TextFiles files) {
        this.files = files;
    }

    public LoadedText execute(Path file) {
        return files.read(file);
    }
}
