package io.github.lukmnh.toolbox.pdf.sign.application;

import io.github.lukmnh.toolbox.pdf.sign.domain.SignatureRepository;

/** Tombol "Hapus TTD tersimpan": menghapus {@code ~/.toolbox/signature.png}. */
public final class ForgetSavedSignatureUseCase {

    private final SignatureRepository repository;

    public ForgetSavedSignatureUseCase(SignatureRepository repository) {
        this.repository = repository;
    }

    public void execute() {
        repository.deleteLast();
    }
}
