package io.github.lukmnh.toolbox.devutils.qr.domain;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;

/**
 * Modul QR persegi, satu sel per modul, sudah termasuk quiet zone. Immutable.
 *
 * @param dimension jumlah modul per sisi (termasuk quiet zone)
 */
public record QrMatrix(int dimension, BitSet dark) {

    public static final int LIGHT_ARGB = 0xFFFF_FFFF;
    public static final int DARK_ARGB = 0xFF00_0000;

    public QrMatrix {
        if (dimension < 1) {
            throw new IllegalArgumentException("Dimensi QR tidak valid: " + dimension);
        }
        dark = (BitSet) Objects.requireNonNull(dark, "dark").clone();
    }

    @Override
    public BitSet dark() {
        return (BitSet) dark.clone();
    }

    public boolean isDark(int x, int y) {
        return dark.get(y * dimension + x);
    }

    /** Sisi gambar hasil {@link #rasterize(int)}: tidak pernah lebih kecil dari 1 px per modul. */
    public int renderedSide(int targetSide) {
        return Math.max(targetSide, dimension);
    }

    /**
     * Render ke piksel ARGB persegi. Tiap modul diperbesar dengan skala bulat supaya tepinya tajam (bisa di-scan);
     * sisa piksel dari pembagian dibagi rata sebagai border putih tambahan, sama seperti renderer ZXing.
     */
    public QrRaster rasterize(int targetSide) {
        int side = renderedSide(targetSide);
        int scale = side / dimension;
        int offset = (side - dimension * scale) / 2;
        int[] pixels = new int[side * side];
        Arrays.fill(pixels, LIGHT_ARGB);
        for (int y = 0; y < dimension; y++) {
            for (int x = 0; x < dimension; x++) {
                if (isDark(x, y)) {
                    fillModule(pixels, side, offset + x * scale, offset + y * scale, scale);
                }
            }
        }
        return new QrRaster(side, pixels);
    }

    private static void fillModule(int[] pixels, int side, int left, int top, int scale) {
        for (int row = top; row < top + scale; row++) {
            int start = row * side + left;
            Arrays.fill(pixels, start, start + scale, DARK_ARGB);
        }
    }
}
