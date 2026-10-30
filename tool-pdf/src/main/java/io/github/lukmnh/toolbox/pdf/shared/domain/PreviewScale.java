package io.github.lukmnh.toolbox.pdf.shared.domain;

/**
 * Konversi pixel preview ↔ point PDF. Preview dirender 144 DPI, PDF memakai 72 point per inch, jadi
 * 1 point = 2 pixel preview.
 */
public final class PreviewScale {

    public static final int PREVIEW_DPI = 144;
    public static final int POINTS_PER_INCH = 72;
    public static final double PIXELS_PER_POINT = (double) PREVIEW_DPI / POINTS_PER_INCH;

    /** Batas atas skala layar saat render: A4 di 288 dpi ≈ 32 MB ARGB, di atas itu heap terlalu boros. */
    public static final double MAX_OUTPUT_SCALE = 2;

    private PreviewScale() {}

    /**
     * DPI render untuk layar dengan skala {@code outputScale} (125% = 1.25), supaya preview tetap tajam di HiDPI.
     * Koordinat preview tetap {@link #PIXELS_PER_POINT}; hanya resolusi gambar yang naik.
     */
    public static int renderDpi(double outputScale) {
        return (int) Math.round(PREVIEW_DPI * Math.clamp(outputScale, 1, MAX_OUTPUT_SCALE));
    }

    public static double toPoints(double previewPixels) {
        return previewPixels / PIXELS_PER_POINT;
    }

    public static double toPixels(double points) {
        return points * PIXELS_PER_POINT;
    }
}
