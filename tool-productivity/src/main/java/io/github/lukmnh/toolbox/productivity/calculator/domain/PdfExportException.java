package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.io.Serial;

public final class PdfExportException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;

    public PdfExportException(String fileName, Throwable cause) {
        super("Gagal menulis PDF " + fileName, cause);
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
