package io.github.lukmnh.toolbox.devutils.sql.domain;

import java.nio.file.Path;

/** Port baca/tulis file {@code .sql}. */
public interface SqlFiles {

    /**
     * Baca file sebagai UTF-8 (BOM dibuang).
     *
     * @throws SqlFileTooLargeException bila melebihi batas
     * @throws SqlIoException bila gagal dibaca
     */
    LoadedSql read(Path file);

    /** @throws SqlIoException bila gagal ditulis */
    void write(Path target, String content);
}
