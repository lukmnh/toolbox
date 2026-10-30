package io.github.lukmnh.toolbox.devutils.qr.domain;

import java.io.Serial;

/** PNG gagal ditulis ke temp sebelum di-download. */
public final class QrExportException extends QrToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;

    public QrExportException(String fileName, Throwable cause) {
        super("Gagal menulis " + fileName, cause);
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
