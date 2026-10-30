package io.github.lukmnh.toolbox.devutils.qr.application;

import io.github.lukmnh.toolbox.devutils.qr.domain.QrEncoder;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrErrorCorrection;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrMatrix;

/** Encode isi (teks, URL, atau vCard yang sudah dirakit) menjadi matriks QR. */
public final class GenerateQrUseCase {

    public static final int MIN_MARGIN = 0;
    public static final int MAX_MARGIN = 16;
    /** Quiet zone minimal menurut spesifikasi QR. */
    public static final int DEFAULT_MARGIN = 4;

    private final QrEncoder encoder;

    public GenerateQrUseCase(QrEncoder encoder) {
        this.encoder = encoder;
    }

    /**
     * @throws IllegalArgumentException bila isi kosong atau margin di luar {@value #MIN_MARGIN}–{@value #MAX_MARGIN}
     * @throws io.github.lukmnh.toolbox.devutils.qr.domain.QrContentTooLargeException bila melebihi kapasitas
     */
    public QrMatrix execute(String payload, QrErrorCorrection errorCorrection, int margin) {
        if (payload.isEmpty()) {
            throw new IllegalArgumentException("Isi QR kosong");
        }
        if (margin < MIN_MARGIN || margin > MAX_MARGIN) {
            throw new IllegalArgumentException("Margin harus " + MIN_MARGIN + "–" + MAX_MARGIN + ": " + margin);
        }
        return encoder.encode(payload, errorCorrection, margin);
    }
}
