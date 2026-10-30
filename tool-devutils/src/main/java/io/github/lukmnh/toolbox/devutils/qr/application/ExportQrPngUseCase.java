package io.github.lukmnh.toolbox.devutils.qr.application;

import io.github.lukmnh.toolbox.devutils.qr.domain.QrMatrix;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrPngWriter;
import java.nio.file.Path;
import java.util.List;

/** Render matriks QR ke PNG berukuran tertentu (temp file) untuk di-download. */
public final class ExportQrPngUseCase {

    /** Pilihan ukuran PNG di UI (sisi, piksel). */
    public static final List<Integer> SIZES = List.of(256, 512, 768, 1024);

    private final QrPngWriter writer;

    public ExportQrPngUseCase(QrPngWriter writer) {
        this.writer = writer;
    }

    /** @throws IllegalArgumentException bila ukuran di luar {@link #SIZES} */
    public void execute(QrMatrix matrix, int sizePx, Path target) {
        if (sizePx < SIZES.getFirst() || sizePx > SIZES.getLast()) {
            throw new IllegalArgumentException("Ukuran PNG harus " + SIZES.getFirst() + "–" + SIZES.getLast() + " px");
        }
        writer.write(matrix.rasterize(sizePx), target);
    }
}
