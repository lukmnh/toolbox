package io.github.lukmnh.toolbox.pdf.shared.domain;

/**
 * Konversi pixel preview ↔ point PDF. Preview dirender 144 DPI, PDF memakai 72 point per inch, jadi
 * 1 point = 2 pixel preview.
 */
public final class PreviewScale {

    public static final int PREVIEW_DPI = 144;
    public static final int POINTS_PER_INCH = 72;
    public static final double PIXELS_PER_POINT = (double) PREVIEW_DPI / POINTS_PER_INCH;

    private PreviewScale() {}

    public static double toPoints(double previewPixels) {
        return previewPixels / PIXELS_PER_POINT;
    }

    public static double toPixels(double points) {
        return points * PIXELS_PER_POINT;
    }
}
