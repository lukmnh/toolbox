package io.github.lukmnh.toolbox.devutils.encoder.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.devutils.encoder.domain.Base64Codec;
import io.github.lukmnh.toolbox.devutils.encoder.domain.Base64Files;
import io.github.lukmnh.toolbox.devutils.encoder.domain.DecodedFile;
import io.github.lukmnh.toolbox.devutils.encoder.domain.FileType;
import java.nio.file.Path;
import java.util.Arrays;

/** Decode Base64 (dari file teks atau hasil paste) menjadi file biner. */
public final class DecodeToFileUseCase {

    private final Base64Files files;

    public DecodeToFileUseCase(Base64Files files) {
        this.files = files;
    }

    /** Streaming: file Base64 berukuran GB tidak pernah dimuat utuh ke memori. */
    public DecodedFile fromFile(Path source, Path target, ProgressReporter progress, CancellationToken cancellation) {
        return files.decode(source, target, (done, total) -> {
            cancellation.throwIfCancelled();
            progress.report(done, total);
        });
    }

    /** Teks hasil paste sudah ada di memori, jadi cukup decode langsung (posisi error = indeks karakter). */
    public DecodedFile fromText(String base64, Path target) {
        byte[] bytes = Base64Codec.decodeBytes(base64);
        files.writeBytes(target, bytes);
        FileType type = FileType.detect(Arrays.copyOf(bytes, Math.min(bytes.length, FileType.HEADER_BYTES)));
        return new DecodedFile(target, bytes.length, type);
    }
}
