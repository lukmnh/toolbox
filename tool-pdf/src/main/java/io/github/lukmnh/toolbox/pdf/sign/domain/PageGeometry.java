package io.github.lukmnh.toolbox.pdf.sign.domain;

/** Ukuran halaman seperti yang terlihat (setelah rotasi), dalam point. */
public record PageGeometry(double width, double height) {

    public PageGeometry {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Ukuran halaman harus positif");
        }
    }
}
