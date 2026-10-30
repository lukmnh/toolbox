package io.github.lukmnh.toolbox.devutils.encoder.domain;

import java.nio.file.Path;
import java.util.Optional;

/** Port ke filesystem untuk mode File: semua operasi streaming supaya aman untuk file besar. */
public interface Base64Files {

    long size(Path file);

    /**
     * Encode {@code source} ke teks Base64 di {@code target}.
     *
     * @param dataUriPrefix ditulis sebelum payload; kosong bila bukan data URI
     */
    EncodedFile encode(
            Path source, Path target, FileEncodeOptions options, String dataUriPrefix, ProgressListener progress);

    /**
     * Decode file teks Base64 (whitespace & prefix data URI diabaikan) ke {@code target}.
     *
     * @throws InvalidBase64Exception dengan posisi byte di {@code source}
     */
    DecodedFile decode(Path source, Path target, ProgressListener progress);

    void writeBytes(Path target, byte[] content);

    TextPreview readPreview(Path file, int maxBytes);

    /** Isi file teks Base64 (ASCII) secara utuh; pemanggil wajib cek ukuran dulu. */
    String readText(Path file);

    /** MIME type menurut OS, kosong bila tidak terdeteksi. */
    Optional<String> probeContentType(Path file);

    /** Dimensi dari header gambar tanpa decode piksel; kosong bila bukan gambar yang dikenali. */
    Optional<ImageSize> imageSize(Path file);
}
