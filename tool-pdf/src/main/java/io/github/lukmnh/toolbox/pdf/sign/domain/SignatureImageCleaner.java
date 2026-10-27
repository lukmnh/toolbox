package io.github.lukmnh.toolbox.pdf.sign.domain;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Menyiapkan gambar tanda tangan hasil upload: downscale bila terlalu lebar, dan untuk gambar tanpa alpha
 * (foto/scan di kertas putih) background dibuat transparan.
 *
 * <p>Caranya: luminance → dibalik menjadi "kepekatan tinta" → contrast stretch antara median (kertas, karena
 * mayoritas pixel adalah kertas) dan persentil 99 (tinta paling pekat) → dipakai sebagai alpha. Warna RGB
 * asli tidak diubah, jadi tinta biru tetap biru.
 */
public final class SignatureImageCleaner {

    public static final int MAX_WIDTH = 1500;

    private static final int MAX_CHANNEL = 255;
    private static final double RED_WEIGHT = 0.299;
    private static final double GREEN_WEIGHT = 0.587;
    private static final double BLUE_WEIGHT = 0.114;
    private static final double BACKGROUND_PERCENTILE = 0.5;
    private static final double INK_PERCENTILE = 0.99;
    private static final int HISTOGRAM_SIZE = MAX_CHANNEL + 1;

    public BufferedImage clean(BufferedImage source) {
        BufferedImage sized = downscaleIfTooWide(source);
        if (sized.getColorModel().hasAlpha()) {
            return toArgb(sized);
        }
        return removeLightBackground(sized);
    }

    private static BufferedImage downscaleIfTooWide(BufferedImage source) {
        if (source.getWidth() <= MAX_WIDTH) {
            return source;
        }
        int height = Math.max(1, (int) Math.round(source.getHeight() * (double) MAX_WIDTH / source.getWidth()));
        int type = source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage scaled = new BufferedImage(MAX_WIDTH, height, type);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, MAX_WIDTH, height, null);
        } finally {
            graphics.dispose();
        }
        return scaled;
    }

    private static BufferedImage toArgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_ARGB) {
            return source;
        }
        BufferedImage argb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = argb.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return argb;
    }

    private static BufferedImage removeLightBackground(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[] rgb = source.getRGB(0, 0, width, height, null, 0, width);
        int[] ink = new int[rgb.length];
        int[] histogram = new int[HISTOGRAM_SIZE];
        for (int index = 0; index < rgb.length; index++) {
            ink[index] = MAX_CHANNEL - luminance(rgb[index]);
            histogram[ink[index]]++;
        }
        int background = percentile(histogram, rgb.length, BACKGROUND_PERCENTILE);
        int strongestInk = percentile(histogram, rgb.length, INK_PERCENTILE);
        int range = Math.max(1, strongestInk - background);

        int[] argb = new int[rgb.length];
        for (int index = 0; index < rgb.length; index++) {
            int alpha = clamp((ink[index] - background) * MAX_CHANNEL / range);
            argb[index] = (alpha << 24) | (rgb[index] & 0x00FFFFFF);
        }
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, width, height, argb, 0, width);
        return result;
    }

    static int luminance(int rgb) {
        int red = (rgb >> 16) & MAX_CHANNEL;
        int green = (rgb >> 8) & MAX_CHANNEL;
        int blue = rgb & MAX_CHANNEL;
        return (int) Math.round(RED_WEIGHT * red + GREEN_WEIGHT * green + BLUE_WEIGHT * blue);
    }

    private static int percentile(int[] histogram, int total, double fraction) {
        long threshold = (long) Math.ceil(total * fraction);
        long cumulative = 0;
        for (int value = 0; value < histogram.length; value++) {
            cumulative += histogram[value];
            if (cumulative >= threshold) {
                return value;
            }
        }
        return histogram.length - 1;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(MAX_CHANNEL, value));
    }
}
