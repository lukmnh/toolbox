package io.github.lukmnh.toolbox.devutils.hash.domain;

import java.io.Serial;

/** File PEM gagal ditulis ke temp sebelum di-download. */
public final class KeyExportException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;

    public KeyExportException(String fileName, Throwable cause) {
        super("Gagal menulis " + fileName, cause);
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
