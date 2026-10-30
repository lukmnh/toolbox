package io.github.lukmnh.toolbox.json.beautify.application;

import io.github.lukmnh.toolbox.json.beautify.domain.JsonFormatter;
import io.github.lukmnh.toolbox.json.beautify.domain.JsonInput;
import io.github.lukmnh.toolbox.json.shared.domain.NotJsonFileException;
import java.nio.file.Path;

/** Menerima file upload untuk Beautify: hanya {@code .json}, ukuran dibaca untuk aturan sort keys. */
public final class LoadJsonFileUseCase {

    private final JsonFormatter formatter;

    public LoadJsonFileUseCase(JsonFormatter formatter) {
        this.formatter = formatter;
    }

    /** @throws NotJsonFileException bila bukan {@code .json} */
    public JsonInput.FileInput execute(Path file) {
        // Cek sebelum menyentuh file system supaya file .jsonl/.txt langsung ditolak
        if (!JsonInput.FileInput.isJsonFile(file)) {
            throw new NotJsonFileException(file);
        }
        return new JsonInput.FileInput(file, formatter.fileSize(file));
    }
}
