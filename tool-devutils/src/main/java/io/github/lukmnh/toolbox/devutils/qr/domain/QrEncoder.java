package io.github.lukmnh.toolbox.devutils.qr.domain;

/** Port ke library encoder QR. Harus thread-safe. */
public interface QrEncoder {

    /**
     * @param payload isi QR, tidak kosong
     * @param margin lebar quiet zone dalam modul
     * @throws QrContentTooLargeException bila isi melebihi kapasitas QR
     */
    QrMatrix encode(String payload, QrErrorCorrection errorCorrection, int margin);
}
