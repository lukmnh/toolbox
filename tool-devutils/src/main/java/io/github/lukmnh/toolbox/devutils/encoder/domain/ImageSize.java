package io.github.lukmnh.toolbox.devutils.encoder.domain;

/** Dimensi gambar yang dibaca dari header, sebelum gambar di-decode. */
public record ImageSize(int width, int height) {

    public ImageSize {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Dimensi gambar harus positif");
        }
    }

    /** Batas per sisi untuk mencegah decompression bomb (Security Requirements #3). */
    public boolean fitsWithin(int maxDimension) {
        return width <= maxDimension && height <= maxDimension;
    }
}
