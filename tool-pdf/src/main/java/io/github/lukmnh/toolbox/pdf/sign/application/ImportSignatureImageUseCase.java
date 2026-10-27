package io.github.lukmnh.toolbox.pdf.sign.application;

import io.github.lukmnh.toolbox.pdf.sign.domain.SignatureImageCleaner;
import io.github.lukmnh.toolbox.pdf.sign.domain.SignatureRepository;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

/** Upload gambar tanda tangan (PNG/JPG): bersihkan background, simpan sebagai tanda tangan terakhir. */
public final class ImportSignatureImageUseCase {

    private final SignatureRepository repository;
    private final SignatureImageCleaner cleaner;

    public ImportSignatureImageUseCase(SignatureRepository repository, SignatureImageCleaner cleaner) {
        this.repository = repository;
        this.cleaner = cleaner;
    }

    public BufferedImage execute(Path imageFile) {
        BufferedImage cleaned = cleaner.clean(repository.readImage(imageFile));
        repository.saveLast(cleaned);
        return cleaned;
    }
}
