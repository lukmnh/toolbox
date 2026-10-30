package io.github.lukmnh.toolbox.pdf.sign.domain;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Elemen yang ditempel di atas halaman. Koordinat dalam point tampilan: origin kiri-atas halaman seperti
 * yang terlihat user (sudah memperhitungkan rotasi), y ke bawah.
 */
public sealed interface Overlay permits Overlay.Text, Overlay.Image {

    int pageIndex();

    /**
     * Teks multi-line. {@code (x, baselineY)} adalah titik awal baseline baris pertama.
     *
     * @param fontSize ukuran font dalam point; jarak antar baris = {@link EditDefaults#LINE_HEIGHT_FACTOR} × fontSize
     */
    record Text(int pageIndex, double x, double baselineY, String text, double fontSize) implements Overlay {
        public Text {
            requireValidPage(pageIndex);
            Objects.requireNonNull(text, "text");
            if (fontSize <= 0) {
                throw new IllegalArgumentException("fontSize must be positive: " + fontSize);
            }
        }

        public String[] lines() {
            return text.split("\\R", -1);
        }
    }

    /** Gambar (tanda tangan). {@code (x, y)} adalah pojok kiri-atas. */
    record Image(int pageIndex, double x, double y, double width, double height, BufferedImage image)
            implements Overlay {
        public Image {
            requireValidPage(pageIndex);
            Objects.requireNonNull(image, "image");
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Image size must be positive: " + width + "x" + height);
            }
        }
    }

    private static void requireValidPage(int pageIndex) {
        if (pageIndex < 0) {
            throw new IllegalArgumentException("pageIndex is negative: " + pageIndex);
        }
    }
}
