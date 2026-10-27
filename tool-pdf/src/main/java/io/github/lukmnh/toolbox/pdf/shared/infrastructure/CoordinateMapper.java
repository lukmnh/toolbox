package io.github.lukmnh.toolbox.pdf.shared.infrastructure;

import io.github.lukmnh.toolbox.pdf.shared.domain.PreviewScale;

/**
 * Memetakan koordinat tampilan (yang dilihat user di preview) ke user space PDF dan sebaliknya.
 *
 * <p>Koordinat tampilan: satuan point, origin kiri-atas, y ke bawah, halaman sudah diputar sesuai
 * {@code /Rotate}. User space PDF: origin kiri-bawah MediaBox, y ke atas, tanpa rotasi, sehingga harus
 * memperhitungkan offset CropBox dan rotasi. Rumus mengikuti transformasi {@code PDFRenderer} PDFBox
 * supaya posisi hasil burn persis sama dengan preview.
 */
public final class CoordinateMapper {

    private final PageBox box;

    public CoordinateMapper(PageBox box) {
        this.box = box;
    }

    public record Point(double x, double y) {}

    /** Lebar halaman seperti yang terlihat (sudah diputar). */
    public double displayWidth() {
        return box.isQuarterTurned() ? box.height() : box.width();
    }

    public double displayHeight() {
        return box.isQuarterTurned() ? box.width() : box.height();
    }

    /**
     * Sudut (derajat, berlawanan jarum jam) untuk transformasi konten yang ditambahkan, supaya konten
     * tampil tegak di halaman yang diputar.
     */
    public double contentRotationDegrees() {
        return box.rotation();
    }

    public Point toPdf(double displayX, double displayY) {
        // u, v: posisi relatif terhadap pojok kiri-bawah CropBox, sebelum rotasi
        double u;
        double v;
        switch (box.rotation()) {
            case 90 -> {
                u = displayY;
                v = displayX;
            }
            case 180 -> {
                u = box.width() - displayX;
                v = displayY;
            }
            case 270 -> {
                u = box.width() - displayY;
                v = box.height() - displayX;
            }
            default -> {
                u = displayX;
                v = box.height() - displayY;
            }
        }
        return new Point(box.lowerLeftX() + u, box.lowerLeftY() + v);
    }

    public Point toDisplay(double pdfX, double pdfY) {
        double u = pdfX - box.lowerLeftX();
        double v = pdfY - box.lowerLeftY();
        return switch (box.rotation()) {
            case 90 -> new Point(v, u);
            case 180 -> new Point(box.width() - u, v);
            case 270 -> new Point(box.height() - v, box.width() - u);
            default -> new Point(u, box.height() - v);
        };
    }

    public Point previewPixelsToPdf(double pixelX, double pixelY) {
        return toPdf(PreviewScale.toPoints(pixelX), PreviewScale.toPoints(pixelY));
    }

    public Point pdfToPreviewPixels(double pdfX, double pdfY) {
        Point display = toDisplay(pdfX, pdfY);
        return new Point(PreviewScale.toPixels(display.x()), PreviewScale.toPixels(display.y()));
    }
}
