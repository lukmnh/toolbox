package io.github.lukmnh.toolbox.pdf.sign.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class SignatureImageCleanerTest {

    private static final int ALPHA_SHIFT = 24;
    private static final int RGB_MASK = 0x00FFFFFF;

    private final SignatureImageCleaner cleaner = new SignatureImageCleaner();

    @Test
    void whitePaperBecomesTransparentAndInkKeepsItsColor() {
        Color blueInk = new Color(0x1A3FA8);
        BufferedImage scan = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scan.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 200, 100);
        graphics.setColor(blueInk);
        graphics.fillRect(50, 40, 100, 10);
        graphics.dispose();

        BufferedImage cleaned = cleaner.clean(scan);

        assertTrue(cleaned.getColorModel().hasAlpha());
        assertEquals(0, alpha(cleaned, 10, 10), "kertas harus transparan");
        assertEquals(255, alpha(cleaned, 100, 45), "tinta harus opaque");
        assertEquals(blueInk.getRGB() & RGB_MASK, cleaned.getRGB(100, 45) & RGB_MASK, "warna tinta dipertahankan");
    }

    @Test
    void imageWithAlphaIsKeptAsIs() {
        BufferedImage png = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        png.setRGB(10, 10, 0x7F000000);

        BufferedImage cleaned = cleaner.clean(png);

        assertSame(png, cleaned);
    }

    @Test
    void wideImageIsDownscaledKeepingAspectRatio() {
        BufferedImage wide = new BufferedImage(3000, 1000, BufferedImage.TYPE_INT_ARGB);

        BufferedImage cleaned = cleaner.clean(wide);

        assertEquals(SignatureImageCleaner.MAX_WIDTH, cleaned.getWidth());
        assertEquals(500, cleaned.getHeight());
    }

    private static int alpha(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) >>> ALPHA_SHIFT;
    }
}
