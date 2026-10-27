package io.github.lukmnh.toolbox.pdf.sign.application;

import io.github.lukmnh.toolbox.pdf.sign.domain.SignatureRasterizer;
import io.github.lukmnh.toolbox.pdf.sign.domain.SignatureRepository;
import io.github.lukmnh.toolbox.pdf.sign.domain.SignatureStroke;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

/** Mengubah goresan dari kanvas menjadi gambar tanda tangan dan menyimpannya. */
public final class SaveDrawnSignatureUseCase {

    private final SignatureRepository repository;
    private final SignatureRasterizer rasterizer;

    public SaveDrawnSignatureUseCase(SignatureRepository repository, SignatureRasterizer rasterizer) {
        this.repository = repository;
        this.rasterizer = rasterizer;
    }

    /** @return kosong bila tidak ada goresan */
    public Optional<BufferedImage> execute(List<SignatureStroke> strokes) {
        Optional<BufferedImage> signature = rasterizer.rasterize(strokes);
        signature.ifPresent(repository::saveLast);
        return signature;
    }
}
