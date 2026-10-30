package io.github.lukmnh.toolbox.devutils.qr.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrContentTooLargeException;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrErrorCorrection;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrMatrix;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrRaster;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ZxingQrEncoderTest {

    private final ZxingQrEncoder encoder = new ZxingQrEncoder();

    @Test
    void matrixSizeIsModulesPlusQuietZone() {
        // "HELLO" muat di versi 1 (21×21 modul)
        QrMatrix matrix = encoder.encode("HELLO", QrErrorCorrection.L, 4);

        assertEquals(21 + 2 * 4, matrix.dimension());
        // Quiet zone selalu putih; finder pattern kiri atas dimulai dengan modul gelap
        assertFalse(matrix.isDark(0, 0));
        assertTrue(matrix.isDark(4, 4));
        assertEquals(21, encoder.encode("HELLO", QrErrorCorrection.L, 0).dimension());
    }

    @Test
    void higherErrorCorrectionNeedsBiggerMatrix() {
        String payload = "https://example.com/promo?utm_source=qr&utm_campaign=launch-2026";

        assertTrue(encoder.encode(payload, QrErrorCorrection.H, 0).dimension()
                > encoder.encode(payload, QrErrorCorrection.L, 0).dimension());
    }

    @Test
    void contentAboveCapacityFailsWithDomainError() {
        String payload = "x".repeat(QrErrorCorrection.H.maxBytes() + 1);

        QrContentTooLargeException error =
                assertThrows(QrContentTooLargeException.class, () -> encoder.encode(payload, QrErrorCorrection.H, 4));
        assertEquals(QrErrorCorrection.H, error.errorCorrection());
        assertEquals(payload.length(), error.payloadBytes());
    }

    @Test
    void rasterScalesWithIntegerModulesAndCentersLeftover() {
        QrMatrix matrix = encoder.encode("HELLO", QrErrorCorrection.L, 4);

        QrRaster raster = matrix.rasterize(300);

        // 29 modul × 10 px = 290 px, sisa 10 px dibagi rata (5 px tiap sisi)
        assertEquals(300, raster.side());
        int topLeftModule = 5 + 4 * 10;
        assertEquals(QrMatrix.DARK_ARGB, raster.argb()[topLeftModule * 300 + topLeftModule]);
        assertEquals(QrMatrix.LIGHT_ARGB, raster.argb()[0]);
        assertEquals(matrix.dimension(), matrix.rasterize(1).side());
    }

    @Test
    void pngDecodesBackToUtf8Payload(@TempDir Path directory) throws Exception {
        String payload = "Halo dunia — QR ✓";
        Path png = directory.resolve("qr.png");

        new ImageIoQrPngWriter()
                .write(encoder.encode(payload, QrErrorCorrection.M, 4).rasterize(512), png);

        BufferedImage image = ImageIO.read(png.toFile());
        assertEquals(512, image.getWidth());
        assertTrue(Files.size(png) > 0);
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        LuminanceSource source = new RGBLuminanceSource(image.getWidth(), image.getHeight(), pixels);
        String decoded = new QRCodeReader()
                .decode(new BinaryBitmap(new HybridBinarizer(source)))
                .getText();
        assertEquals(payload, decoded);
    }
}
