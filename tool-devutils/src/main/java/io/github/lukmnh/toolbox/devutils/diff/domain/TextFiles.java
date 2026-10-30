package io.github.lukmnh.toolbox.devutils.diff.domain;

import java.nio.file.Path;

/** Port baca/tulis file teks untuk Text Diff. */
public interface TextFiles {

    /**
     * Baca file sebagai UTF-8 (BOM dibuang).
     *
     * @throws DiffFileTooLargeException bila melebihi batas
     * @throws BinaryFileException bila file terdeteksi biner
     * @throws DiffIoException bila gagal dibaca
     */
    LoadedText read(Path file);

    /** @throws DiffIoException bila gagal ditulis */
    void write(Path target, String content);
}
