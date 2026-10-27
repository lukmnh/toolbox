package io.github.lukmnh.toolbox.pdf.sign.application;

import io.github.lukmnh.toolbox.pdf.sign.domain.SignatureRepository;
import java.awt.image.BufferedImage;
import java.util.Optional;

/** Memuat tanda tangan terakhir, dipanggil otomatis saat tool dibuka. */
public final class LoadLastSignatureUseCase {

    private final SignatureRepository repository;

    public LoadLastSignatureUseCase(SignatureRepository repository) {
        this.repository = repository;
    }

    public Optional<BufferedImage> execute() {
        return repository.loadLast();
    }
}
