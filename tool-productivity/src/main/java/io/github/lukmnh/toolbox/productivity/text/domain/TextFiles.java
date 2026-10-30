package io.github.lukmnh.toolbox.productivity.text.domain;

import java.nio.file.Path;

/** Port baca/tulis file {@code .txt} untuk Text Tools. */
public interface TextFiles {

    /**
     * Baca file sebagai UTF-8 (BOM dibuang, {@code \r\n}/{@code \r} jadi {@code \n}).
     *
     * @throws TextFileTooLargeException bila melebihi batas Text Tools
     * @throws TextIoException bila gagal dibaca
     */
    LoadedText read(Path file);

    /** Tulis teks sebagai UTF-8. @throws TextIoException bila gagal ditulis */
    void write(Path target, String content);
}
