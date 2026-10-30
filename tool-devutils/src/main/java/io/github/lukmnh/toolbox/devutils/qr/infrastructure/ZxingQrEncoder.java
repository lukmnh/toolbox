package io.github.lukmnh.toolbox.devutils.qr.infrastructure;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrContentTooLargeException;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrEncoder;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrErrorCorrection;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrMatrix;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** Encoder QR ZXing core. Stateless dan thread-safe ({@link QRCodeWriter} dibuat per panggilan). */
public final class ZxingQrEncoder implements QrEncoder {

    @Override
    public QrMatrix encode(String payload, QrErrorCorrection errorCorrection, int margin) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.valueOf(errorCorrection.name()));
        hints.put(EncodeHintType.MARGIN, margin);
        // ECI UTF-8 hanya bila perlu: scanner lama kadang salah membaca QR ASCII yang diberi header ECI
        if (!StandardCharsets.US_ASCII.newEncoder().canEncode(payload)) {
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        }
        try {
            // Ukuran 0 = satu piksel per modul (plus quiet zone); skala dilakukan saat render
            BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 0, 0, hints);
            return toDomain(matrix);
        } catch (WriterException e) {
            if (isCapacityError(e)) {
                throw new QrContentTooLargeException(
                        payload.getBytes(StandardCharsets.UTF_8).length, errorCorrection, e);
            }
            throw new IllegalStateException("ZXing gagal meng-encode QR: " + e.getMessage(), e);
        }
    }

    /** ZXing tidak punya tipe khusus untuk kapasitas; pesannya selalu "Data too big…". */
    private static boolean isCapacityError(WriterException error) {
        String message = error.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("too big");
    }

    private static QrMatrix toDomain(BitMatrix matrix) {
        int dimension = matrix.getWidth();
        BitSet dark = new BitSet(dimension * dimension);
        for (int y = 0; y < dimension; y++) {
            for (int x = 0; x < dimension; x++) {
                if (matrix.get(x, y)) {
                    dark.set(y * dimension + x);
                }
            }
        }
        return new QrMatrix(dimension, dark);
    }
}
