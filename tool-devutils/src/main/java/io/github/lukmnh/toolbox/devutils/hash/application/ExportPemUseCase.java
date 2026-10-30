package io.github.lukmnh.toolbox.devutils.hash.application;

import io.github.lukmnh.toolbox.devutils.hash.domain.PemFiles;
import java.nio.file.Path;

/** Tulis PEM ke temp file untuk di-download. */
public final class ExportPemUseCase {

    private final PemFiles files;

    public ExportPemUseCase(PemFiles files) {
        this.files = files;
    }

    public void execute(String pem, Path target) {
        if (pem.isEmpty()) {
            throw new IllegalArgumentException("Tidak ada key untuk diekspor");
        }
        files.write(target, pem);
    }
}
