package io.github.lukmnh.toolbox.pdf.shared.domain;

import java.io.Serial;
import java.nio.file.Path;

/** File bukan PDF yang valid atau rusak. */
public final class CorruptPdfException extends PdfToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Path file;

    public CorruptPdfException(Path file, Throwable cause) {
        super("Invalid PDF: " + file, cause);
        this.file = file;
    }

    public Path file() {
        return file;
    }
}
