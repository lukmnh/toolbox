package io.github.lukmnh.toolbox.pdf.sign.domain;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Optional;

/** Port: membaca gambar upload dan menyimpan tanda tangan terakhir ({@code ~/.toolbox/signature.png}). */
public interface SignatureRepository {

    Optional<BufferedImage> loadLast();

    void saveLast(BufferedImage signature);

    /** @throws io.github.lukmnh.toolbox.pdf.shared.domain.PdfIoException bila file bukan PNG/JPG yang bisa dibaca */
    BufferedImage readImage(Path file);
}
