package io.github.lukmnh.toolbox.devutils.diff.application;

import io.github.lukmnh.toolbox.devutils.diff.domain.TextFiles;
import io.github.lukmnh.toolbox.devutils.diff.domain.UnifiedDiff;
import java.nio.file.Path;

/** Tulis unified diff ke file (temp) untuk di-download sebagai {@code .diff}. */
public final class ExportUnifiedDiffUseCase {

    private final TextFiles files;

    public ExportUnifiedDiffUseCase(TextFiles files) {
        this.files = files;
    }

    public void execute(UnifiedDiff diff, Path target) {
        if (diff.isEmpty()) {
            throw new IllegalArgumentException("Tidak ada perbedaan untuk diekspor");
        }
        files.write(target, diff.text());
    }
}
