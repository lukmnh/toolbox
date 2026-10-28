package io.github.lukmnh.toolbox.devutils.checksum.domain;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.LongConsumer;

/** Port ke filesystem untuk tool Checksum. Semua error I/O dilempar sebagai {@link ChecksumIoException}. */
public interface ChecksumFiles {

    long size(Path file);

    boolean isRegularFile(Path file);

    /**
     * Hitung checksum secara streaming (aman untuk file berukuran GB).
     *
     * @param bytesRead dipanggil setelah tiap blok dengan jumlah byte blok itu; boleh melempar exception untuk
     *     membatalkan
     * @return checksum hex huruf kecil
     */
    String checksum(Path file, ChecksumAlgorithm algorithm, LongConsumer bytesRead);

    /**
     * Resolve nama file dari isi file daftar checksum terhadap {@code directory}.
     *
     * @return kosong bila absolut atau keluar folder
     */
    Optional<Path> resolveInside(Path directory, String relativeName);

    /** @throws SumsFileTooLargeException bila melebihi batas keamanan */
    String readSumsText(Path file);

    void writeText(Path file, String text);
}
