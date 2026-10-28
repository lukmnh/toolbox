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

    private static final SignatureStroke STROKE =
            new SignatureStroke(List.of(new StrokePoint(0, 0), new StrokePoint(30, 10)), InkColor.BLACK, 3);

    @Test
    void drawnSignatureIsSavedWhenUserOptsIn() {
        SaveDrawnSignatureUseCase save = new SaveDrawnSignatureUseCase(repository, new SignatureRasterizer());

        BufferedImage saved = save.execute(List.of(STROKE), true).orElseThrow();

        assertSame(saved, new LoadLastSignatureUseCase(repository).execute().orElseThrow());
    }

    @Test
    void drawnSignatureIsNotSavedByDefault() {
        SaveDrawnSignatureUseCase save = new SaveDrawnSignatureUseCase(repository, new SignatureRasterizer());

        assertTrue(save.execute(List.of(STROKE), false).isPresent());
        assertTrue(repository.last.isEmpty());
    }

    @Test
    void emptyDrawingSavesNothing() {
        new SaveDrawnSignatureUseCase(repository, new SignatureRasterizer()).execute(List.of(), true);

        assertTrue(repository.last.isEmpty());
    }

    @Test
    void uploadedImageIsCleanedThenSavedWhenUserOptsIn() {
        repository.uploadContent = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
        ImportSignatureImageUseCase importer = new ImportSignatureImageUseCase(repository, new SignatureImageCleaner());

        BufferedImage imported = importer.execute(Path.of("ttd.jpg"), true);

        assertTrue(imported.getColorModel().hasAlpha());
        assertEquals(Optional.of(imported), repository.last);
    }

    @Test
    void uploadedImageIsNotSavedByDefault() {
        repository.uploadContent = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);

        new ImportSignatureImageUseCase(repository, new SignatureImageCleaner()).execute(Path.of("ttd.png"), false);

        assertTrue(repository.last.isEmpty());
    }

    @Test
    void forgetDeletesSavedSignature() {
        repository.saveLast(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB));

        new ForgetSavedSignatureUseCase(repository).execute();

        assertTrue(repository.last.isEmpty());
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
        public void deleteLast() {
            last = Optional.empty();
        }

        @Override
        public BufferedImage readImage(Path file) {
            return uploadContent;
        }
    }
}
