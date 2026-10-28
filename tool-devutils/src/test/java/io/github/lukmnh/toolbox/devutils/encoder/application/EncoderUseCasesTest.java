package io.github.lukmnh.toolbox.devutils.encoder.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.common.task.TaskCancelledException;
import io.github.lukmnh.toolbox.devutils.encoder.domain.Base64Limits;
import io.github.lukmnh.toolbox.devutils.encoder.domain.Base64Variant;
import io.github.lukmnh.toolbox.devutils.encoder.domain.DecodedFile;
import io.github.lukmnh.toolbox.devutils.encoder.domain.EncodedFile;
import io.github.lukmnh.toolbox.devutils.encoder.domain.FileEncodeOptions;
import io.github.lukmnh.toolbox.devutils.encoder.domain.FileType;
import io.github.lukmnh.toolbox.devutils.encoder.domain.ImageSize;
import io.github.lukmnh.toolbox.devutils.encoder.domain.InvalidBase64Exception;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EncoderUseCasesTest {

    private static final Path SOURCE = Path.of("foto.png");
    private static final Path TARGET = Path.of("out");
    private static final FileEncodeOptions DATA_URI = new FileEncodeOptions(Base64Variant.STANDARD, true, true);

    private final FakeBase64Files files = new FakeBase64Files();

    @Test
    void dataUriUsesDetectedMimeType() {
        files.probedType = Optional.of("image/webp");

        new EncodeFileUseCase(files).execute(SOURCE, TARGET, DATA_URI, ProgressReporter.NONE, CancellationToken.none());

        assertEquals("data:image/webp;base64,", files.lastPrefix);
    }

    @Test
    void dataUriFallsBackToExtension() {
        new EncodeFileUseCase(files).execute(SOURCE, TARGET, DATA_URI, ProgressReporter.NONE, CancellationToken.none());

        assertEquals("data:image/png;base64,", files.lastPrefix);
    }

    @Test
    void plainEncodingHasNoPrefix() {
        FileEncodeOptions plain = new FileEncodeOptions(Base64Variant.URL_SAFE, false, false);

        new EncodeFileUseCase(files).execute(SOURCE, TARGET, plain, ProgressReporter.NONE, CancellationToken.none());

        assertEquals("", files.lastPrefix);
    }

    @Test
    void cancellationStopsEncoding() {
        CancellationToken token = CancellationToken.none();
        token.cancel();

        assertThrows(
                TaskCancelledException.class,
                () -> new EncodeFileUseCase(files).execute(SOURCE, TARGET, DATA_URI, ProgressReporter.NONE, token));
    }

    @Test
    void pastedBase64IsDecodedAndTyped() {
        DecodedFile decoded = new DecodeToFileUseCase(files).fromText("data:application/pdf;base64,JVBERi0x", TARGET);

        assertArrayEquals("%PDF-1".getBytes(StandardCharsets.US_ASCII), files.written.get(TARGET));
        assertEquals(FileType.PDF, decoded.type());
        assertEquals("decoded.pdf", decoded.suggestedName());
    }

    @Test
    void invalidPasteIsRejectedBeforeWriting() {
        assertThrows(InvalidBase64Exception.class, () -> new DecodeToFileUseCase(files).fromText("ab$c", TARGET));
        assertTrue(files.written.isEmpty());
    }

    @Test
    void copyIsRefusedAboveLimit() {
        ReadFileUseCase read = new ReadFileUseCase(files);
        EncodedFile small = new EncodedFile(TARGET, 1, Base64Limits.COPY_MAX_BYTES);
        EncodedFile large = new EncodedFile(TARGET, 1, Base64Limits.COPY_MAX_BYTES + 1);

        assertEquals("full", read.fullText(small));
        assertThrows(IllegalArgumentException.class, () -> read.fullText(large));
    }

    @Test
    void imageSizeOnlyForPreviewableTypes() {
        files.imageSize = Optional.of(new ImageSize(10, 10));
        ReadFileUseCase read = new ReadFileUseCase(files);

        assertTrue(read.imageSize(new DecodedFile(TARGET, 1, FileType.PNG)).isPresent());
        assertTrue(read.imageSize(new DecodedFile(TARGET, 1, FileType.WEBP)).isEmpty());
    }
}
