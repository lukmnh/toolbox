package io.github.lukmnh.toolbox.pdf.sign.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.pdf.sign.domain.InkColor;
import io.github.lukmnh.toolbox.pdf.sign.domain.SignatureImageCleaner;
import io.github.lukmnh.toolbox.pdf.sign.domain.SignatureRasterizer;
import io.github.lukmnh.toolbox.pdf.sign.domain.SignatureRepository;
import io.github.lukmnh.toolbox.pdf.sign.domain.SignatureStroke;
import io.github.lukmnh.toolbox.pdf.sign.domain.StrokePoint;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SignatureUseCasesTest {

    private final InMemorySignatures repository = new InMemorySignatures();

    @Test
    void drawnSignatureIsSavedAsLastSignature() {
        SaveDrawnSignatureUseCase save = new SaveDrawnSignatureUseCase(repository, new SignatureRasterizer());
        SignatureStroke stroke =
                new SignatureStroke(List.of(new StrokePoint(0, 0), new StrokePoint(30, 10)), InkColor.BLACK, 3);

        BufferedImage saved = save.execute(List.of(stroke)).orElseThrow();

        assertSame(saved, new LoadLastSignatureUseCase(repository).execute().orElseThrow());
    }

    @Test
    void emptyDrawingSavesNothing() {
        new SaveDrawnSignatureUseCase(repository, new SignatureRasterizer()).execute(List.of());

        assertTrue(repository.last.isEmpty());
    }

    @Test
    void uploadedImageIsCleanedThenSaved() {
        repository.uploadContent = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
        ImportSignatureImageUseCase importer = new ImportSignatureImageUseCase(repository, new SignatureImageCleaner());

        BufferedImage imported = importer.execute(Path.of("ttd.jpg"));

        assertTrue(imported.getColorModel().hasAlpha());
        assertEquals(Optional.of(imported), repository.last);
    }

    private static final class InMemorySignatures implements SignatureRepository {
        Optional<BufferedImage> last = Optional.empty();
        BufferedImage uploadContent;

        @Override
        public Optional<BufferedImage> loadLast() {
            return last;
        }

        @Override
        public void saveLast(BufferedImage signature) {
            last = Optional.of(signature);
        }

        @Override
        public BufferedImage readImage(Path file) {
            return uploadContent;
        }
    }
}
