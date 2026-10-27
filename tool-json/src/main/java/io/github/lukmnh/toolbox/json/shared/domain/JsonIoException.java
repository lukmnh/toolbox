package io.github.lukmnh.toolbox.json.shared.domain;

import java.io.Serial;
import java.nio.file.Path;

/** Gagal membaca atau menulis file. */
public final class JsonIoException extends JsonToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Path file;

    public JsonIoException(Path file, Throwable cause) {
        super("Gagal mengakses file: " + file, cause);
        this.file = file;
    }

    public Path file() {
        return file;
    }
}
