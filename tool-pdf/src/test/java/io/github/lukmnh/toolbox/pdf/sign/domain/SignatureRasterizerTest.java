package io.github.lukmnh.toolbox.pdf.sign.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.Test;

class SignatureRasterizerTest {

    private static final int ALPHA_SHIFT = 24;

    private final SignatureRasterizer rasterizer = new SignatureRasterizer();

    @Test
    void noStrokesProducesNoImage() {
        assertTrue(rasterizer.rasterize(List.of()).isEmpty());
    }

    @Test
    void imageIsCroppedToStrokeBoundsPlusPaddingAtTripleScale() {
        SignatureStroke line = new SignatureStroke(
                List.of(new StrokePoint(100, 50), new StrokePoint(150, 50), new StrokePoint(200, 50)),
                InkColor.BLACK,
                4);

        BufferedImage image = rasterizer.rasterize(List.of(line)).orElseThrow();

        double margin = 4 / 2.0 + SignatureRasterizer.PADDING;
        assertEquals((int) Math.ceil((100 + 2 * margin) * SignatureRasterizer.EXPORT_SCALE), image.getWidth());
        assertEquals((int) Math.ceil((2 * margin) * SignatureRasterizer.EXPORT_SCALE), image.getHeight());
    }

    @Test
    void backgroundIsTransparentAndStrokeIsOpaqueInChosenColor() {
        SignatureStroke line =
                new SignatureStroke(List.of(new StrokePoint(0, 0), new StrokePoint(60, 0)), InkColor.BLUE, 6);

        BufferedImage image = rasterizer.rasterize(List.of(line)).orElseThrow();

        int centerY = image.getHeight() / 2;
        int centerX = image.getWidth() / 2;
        assertEquals(0, image.getRGB(1, 1) >>> ALPHA_SHIFT);
        assertEquals(255, image.getRGB(centerX, centerY) >>> ALPHA_SHIFT);
        assertEquals(InkColor.BLUE.rgb(), image.getRGB(centerX, centerY) & 0xFFFFFF);
    }

    @Test
    void singleClickDrawsADot() {
        SignatureStroke dot = new SignatureStroke(List.of(new StrokePoint(10, 10)), InkColor.BLACK, 5);

        BufferedImage image = rasterizer.rasterize(List.of(dot)).orElseThrow();

        assertEquals(255, image.getRGB(image.getWidth() / 2, image.getHeight() / 2) >>> ALPHA_SHIFT);
    }
}
