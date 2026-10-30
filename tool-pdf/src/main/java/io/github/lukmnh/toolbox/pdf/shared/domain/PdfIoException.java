package io.github.lukmnh.toolbox.pdf.shared.domain;

import java.io.Serial;
import java.nio.file.Path;

/** Gagal membaca atau menulis file. */
public final class PdfIoException extends PdfToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Path file;

    public PdfIoException(Path file, Throwable cause) {
        super("Could not access file: " + file, cause);
        this.file = file;
    }

    public Path file() {
        return file;
    }
}
