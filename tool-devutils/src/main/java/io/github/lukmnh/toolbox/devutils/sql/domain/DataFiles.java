package io.github.lukmnh.toolbox.devutils.sql.domain;

import java.nio.file.Path;
import java.util.Set;

/** Port baca data SQL Generator ({@code .xlsx}, {@code .csv}, {@code .txt}, {@code .json}). */
public interface DataFiles {

    /** Ekstensi yang didukung, huruf kecil tanpa titik. */
    Set<String> EXTENSIONS = Set.of("xlsx", "csv", "txt", "json");

    /**
     * Baca baris pertama sebagai header (JSON: key object) dan sisanya sebagai data.
     *
     * @throws InvalidDataException bila format tidak didukung atau isi tidak valid
     * @throws SqlFileTooLargeException bila melebihi batas ukuran
     * @throws SqlIoException bila gagal dibaca
     */
    DataTable read(Path file);
}
