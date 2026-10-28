package io.github.lukmnh.toolbox.devutils.encoder.application;

import io.github.lukmnh.toolbox.devutils.encoder.domain.Base64Files;
import io.github.lukmnh.toolbox.devutils.encoder.domain.Base64Limits;
import io.github.lukmnh.toolbox.devutils.encoder.domain.DecodedFile;
import io.github.lukmnh.toolbox.devutils.encoder.domain.EncodedFile;
import io.github.lukmnh.toolbox.devutils.encoder.domain.ImageSize;
import io.github.lukmnh.toolbox.devutils.encoder.domain.TextPreview;
import java.nio.file.Path;
import java.util.Optional;

/** Baca info dan isi hasil mode File untuk ditampilkan: ukuran, preview teks, teks utuh (copy), dimensi gambar. */
public final class ReadFileUseCase {

    private final Base64Files files;

    public ReadFileUseCase(Base64Files files) {
        this.files = files;
    }

    public long size(Path file) {
        return files.size(file);
    }

    /** Maksimal {@link Base64Limits#PREVIEW_BYTES} pertama supaya TextArea tetap responsif. */
    public TextPreview preview(EncodedFile encoded) {
        return files.readPreview(encoded.output(), Base64Limits.PREVIEW_BYTES);
    }

    /** @throws IllegalArgumentException bila hasil melebihi batas copy */
    public String fullText(EncodedFile encoded) {
        if (!Base64Limits.canCopy(encoded.outputBytes())) {
            throw new IllegalArgumentException("Hasil terlalu besar untuk di-copy");
        }
        return files.readText(encoded.output());
    }

    /** Dimensi hanya untuk gambar yang bisa di-preview; pemanggil wajib cek batas dimensi sebelum decode piksel. */
    public Optional<ImageSize> imageSize(DecodedFile decoded) {
        return decoded.type().previewable() ? files.imageSize(decoded.output()) : Optional.empty();
    }
}
