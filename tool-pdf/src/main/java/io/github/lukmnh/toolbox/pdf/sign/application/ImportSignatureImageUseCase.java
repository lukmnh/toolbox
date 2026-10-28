package io.github.lukmnh.toolbox.pdf.sign.application;

import io.github.lukmnh.toolbox.pdf.sign.domain.SignatureImageCleaner;
import io.github.lukmnh.toolbox.pdf.sign.domain.SignatureRepository;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

/** Upload gambar tanda tangan (PNG/JPG): bersihkan background; disimpan ke disk hanya bila user memilih. */
public final class ImportSignatureImageUseCase {

    private final SignatureRepository repository;
    private final SignatureImageCleaner cleaner;

    public ImportSignatureImageUseCase(SignatureRepository repository, SignatureImageCleaner cleaner) {
        this.repository = repository;
        this.cleaner = cleaner;
    }

    /** @param remember {@code true} bila checkbox "Simpan TTD untuk dipakai lagi" aktif (default mati) */
    public BufferedImage execute(Path imageFile, boolean remember) {
        BufferedImage cleaned = cleaner.clean(repository.readImage(imageFile));
        if (remember) {
            repository.saveLast(cleaned);
        }
        return cleaned;
    }
}
