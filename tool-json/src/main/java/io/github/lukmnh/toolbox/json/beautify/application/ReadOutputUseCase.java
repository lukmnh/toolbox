package io.github.lukmnh.toolbox.json.beautify.application;

import io.github.lukmnh.toolbox.json.beautify.domain.BeautifyLimits;
import io.github.lukmnh.toolbox.json.beautify.domain.FormatResult;
import io.github.lukmnh.toolbox.json.beautify.domain.JsonFormatter;
import io.github.lukmnh.toolbox.json.beautify.domain.TextPreview;
import java.nio.file.Path;

/** Membaca file hasil format untuk preview (1 MB pertama) atau Copy (≤ 10 MB). */
public final class ReadOutputUseCase {

    private final JsonFormatter formatter;

    public ReadOutputUseCase(JsonFormatter formatter) {
        this.formatter = formatter;
    }

    public TextPreview preview(Path output) {
        return formatter.readText(output, BeautifyLimits.PREVIEW_MAX_BYTES);
    }

    /** @throws IllegalStateException bila hasil terlalu besar untuk clipboard atau tidak ada file */
    public String fullText(FormatResult result) {
        Path output = result.outputFile().orElseThrow(() -> new IllegalStateException("Tidak ada file hasil"));
        if (!BeautifyLimits.canCopy(result.outputBytes())) {
            throw new IllegalStateException("Hasil lebih dari 10 MB, gunakan Download");
        }
        return formatter.readText(output, (int) BeautifyLimits.COPY_MAX_BYTES).text();
    }
}
