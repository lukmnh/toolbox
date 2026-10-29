package io.github.lukmnh.toolbox.devutils.regex.application;

import io.github.lukmnh.toolbox.devutils.regex.domain.LoadedTestText;
import io.github.lukmnh.toolbox.devutils.regex.domain.TestTextFiles;
import java.nio.file.Path;

/** Muat teks uji dari file {@code .txt}/{@code .log} (maks 5 MB). */
public final class LoadTestTextUseCase {

    private final TestTextFiles files;

    public LoadTestTextUseCase(TestTextFiles files) {
        this.files = files;
    }

    public LoadedTestText execute(Path file) {
        return files.read(file);
    }
}
