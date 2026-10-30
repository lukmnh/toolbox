package io.github.lukmnh.toolbox.devutils.qr.domain;

import java.io.Serial;

/** Error domain QR Code Generator; sealed supaya pemetaan pesan di presentation exhaustive. */
public abstract sealed class QrToolException extends RuntimeException
        permits QrContentTooLargeException, InvalidQrUrlException, QrExportException {

    @Serial
    private static final long serialVersionUID = 1L;

    protected QrToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
