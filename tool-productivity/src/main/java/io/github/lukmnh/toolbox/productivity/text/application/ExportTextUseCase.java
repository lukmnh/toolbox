package io.github.lukmnh.toolbox.productivity.text.application;

import io.github.lukmnh.toolbox.productivity.text.domain.TextFiles;
import java.nio.file.Path;

/** Tulis teks ke file (temp) untuk di-download sebagai {@code .txt}. */
public final class ExportTextUseCase {

    private final TextFiles files;

    public ExportTextUseCase(TextFiles files) {
        this.files = files;
    }

    /** @throws IllegalArgumentException bila teks kosong */
    public void execute(String text, Path target) {
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Tidak ada teks untuk diekspor");
        }
        files.write(target, text);
    }
}
