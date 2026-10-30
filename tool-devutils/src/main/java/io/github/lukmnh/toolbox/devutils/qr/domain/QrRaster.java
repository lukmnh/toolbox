package io.github.lukmnh.toolbox.devutils.qr.domain;

/**
 * Hasil render {@link QrMatrix}: piksel ARGB persegi, baris demi baris. Buffer sekali pakai untuk
 * {@code BufferedImage}/{@code WritableImage}; array sengaja tidak disalin karena bisa sampai 4 MB.
 *
 * @param side lebar = tinggi dalam piksel
 * @param argb panjang {@code side * side}
 */
public record QrRaster(int side, int[] argb) {

    public QrRaster {
        if (side < 1 || argb.length != side * side) {
            throw new IllegalArgumentException("Raster size mismatch: " + side + " vs " + argb.length);
        }
    }
}
