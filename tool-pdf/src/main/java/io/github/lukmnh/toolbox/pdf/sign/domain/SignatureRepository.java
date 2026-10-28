package io.github.lukmnh.toolbox.pdf.sign.domain;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Port: membaca gambar upload dan menyimpan tanda tangan terakhir ({@code ~/.toolbox/signature.png}, owner-only).
 * Penyimpanan bersifat opt-in (Security Requirements #1); keputusan ada di use case.
 */
public interface SignatureRepository {

    Optional<BufferedImage> loadLast();

    void saveLast(BufferedImage signature);

    /** Hapus tanda tangan tersimpan; tidak error bila memang belum ada. */
    void deleteLast();

    /**
     * @throws io.github.lukmnh.toolbox.pdf.shared.domain.PdfIoException bila file bukan PNG/JPG yang bisa dibaca
     * @throws io.github.lukmnh.toolbox.pdf.shared.domain.PdfLimitExceededException bila file atau dimensi gambar
     *     melebihi batas (dicek dari header sebelum decode)
     */
    BufferedImage readImage(Path file);
}
