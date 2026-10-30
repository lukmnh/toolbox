package io.github.lukmnh.toolbox.devutils.qr.domain;

import java.io.Serial;

/** Isi melebihi kapasitas QR versi 40 pada level error correction yang dipilih. */
public final class QrContentTooLargeException extends QrToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int payloadBytes;
    private final QrErrorCorrection errorCorrection;

    public QrContentTooLargeException(int payloadBytes, QrErrorCorrection errorCorrection, Throwable cause) {
        super("Content of " + payloadBytes + " bytes is too large for QR level " + errorCorrection, cause);
        this.payloadBytes = payloadBytes;
        this.errorCorrection = errorCorrection;
    }

    /** Panjang UTF-8 isi. */
    public int payloadBytes() {
        return payloadBytes;
    }

    public QrErrorCorrection errorCorrection() {
        return errorCorrection;
    }
}
