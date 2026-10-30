package io.github.lukmnh.toolbox.json.shared.domain;

import java.io.Serial;
import java.nio.file.Path;

/** File yang dipilih bukan berekstensi {@code .json} (aturan BR-JS-1). */
public final class NotJsonFileException extends JsonToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Path file;

    public NotJsonFileException(Path file) {
        super("Not a .json file: " + file, null);
        this.file = file;
    }

    public Path file() {
        return file;
    }
}
