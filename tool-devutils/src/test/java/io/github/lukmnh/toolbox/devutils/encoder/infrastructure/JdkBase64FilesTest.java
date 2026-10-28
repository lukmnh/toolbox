package io.github.lukmnh.toolbox.devutils.encoder.infrastructure;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.devutils.encoder.domain.Base64Variant;
import io.github.lukmnh.toolbox.devutils.encoder.domain.DecodedFile;
import io.github.lukmnh.toolbox.devutils.encoder.domain.EncodedFile;
import io.github.lukmnh.toolbox.devutils.encoder.domain.EncoderIoException;
import io.github.lukmnh.toolbox.devutils.encoder.domain.FileEncodeOptions;
import io.github.lukmnh.toolbox.devutils.encoder.domain.FileType;
import io.github.lukmnh.toolbox.devutils.encoder.domain.ImageSize;
import io.github.lukmnh.toolbox.devutils.encoder.domain.InvalidBase64Exception;
import io.github.lukmnh.toolbox.devutils.encoder.domain.ProgressListener;
import io.github.lukmnh.toolbox.devutils.encoder.domain.TextPreview;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Random;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JdkBase64FilesTest {

    private static final ProgressListener NO_PROGRESS = (done, total) -> {};
    private static final FileEncodeOptions STANDARD = new FileEncodeOptions(Base64Variant.STANDARD, true, false);

    private final JdkBase64Files files = new JdkBase64Files();

    @TempDir
    Path directory;

    @Test
    void largeBinaryFileRoundTrips() throws IOException {
        byte[] content = new byte[300_000];
        new Random(42).nextBytes(content);
        Path source = Files.write(directory.resolve("data.bin"), content);
        Path encoded = directory.resolve("data.b64.txt");
        Path decoded = directory.resolve("decoded.bin");

        EncodedFile encodedFile = files.encode(source, encoded, STANDARD, "", NO_PROGRESS);
        DecodedFile decodedFile = files.decode(encoded, decoded, NO_PROGRESS);

        assertEquals(content.length, encodedFile.inputBytes());
        assertEquals(Files.size(encoded), encodedFile.outputBytes());
        assertEquals(Base64.getEncoder().encodeToString(content), Files.readString(encoded));
        assertEquals(content.length, decodedFile.outputBytes());
        assertArrayEquals(content, Files.readAllBytes(decoded));
    }

    @Test
    void mimeAndUrlSafeOutputDecodeBack() throws IOException {
        byte[] content = new byte[1_000];
        new Random(7).nextBytes(content);
        Path source = Files.write(directory.resolve("a.bin"), content);
        for (Base64Variant variant : Base64Variant.values()) {
            Path encoded = directory.resolve(variant + ".txt");
            files.encode(source, encoded, new FileEncodeOptions(variant, false, false), "", NO_PROGRESS);
            Path decoded = directory.resolve(variant + ".bin");

            files.decode(encoded, decoded, NO_PROGRESS);

            assertArrayEquals(content, Files.readAllBytes(decoded), variant.name());
        }
    }

    @Test
    void dataUriPrefixIsWrittenAndStrippedOnDecode() throws IOException {
        byte[] png = pngBytes(4, 3);
        Path source = Files.write(directory.resolve("logo.png"), png);
        Path encoded = directory.resolve("logo.b64.txt");
        FileEncodeOptions dataUri = new FileEncodeOptions(Base64Variant.URL_SAFE, false, true);

        files.encode(source, encoded, dataUri, "data:image/png;base64,", NO_PROGRESS);
        DecodedFile decoded = files.decode(encoded, directory.resolve("out.bin"), NO_PROGRESS);

        assertTrue(Files.readString(encoded).startsWith("data:image/png;base64,iVBOR"));
        assertEquals(FileType.PNG, decoded.type());
        assertArrayEquals(png, Files.readAllBytes(decoded.output()));
        assertEquals(new ImageSize(4, 3), files.imageSize(decoded.output()).orElseThrow());
    }

    @Test
    void invalidCharacterInFileReportsBytePosition() throws IOException {
        Path source = Files.writeString(directory.resolve("bad.b64"), "Zm9v\nYm#Fy");

        InvalidBase64Exception error = assertThrows(
                InvalidBase64Exception.class, () -> files.decode(source, directory.resolve("x.bin"), NO_PROGRESS));

        assertEquals(InvalidBase64Exception.Reason.INVALID_CHARACTER, error.reason());
        assertEquals(7, error.position());
        assertEquals('#', error.character());
    }

    @Test
    void danglingCharacterInFileIsTruncated() throws IOException {
        Path source = Files.writeString(directory.resolve("short.b64"), "Zm9vY\n");

        InvalidBase64Exception error = assertThrows(
                InvalidBase64Exception.class, () -> files.decode(source, directory.resolve("x.bin"), NO_PROGRESS));

        assertEquals(InvalidBase64Exception.Reason.TRUNCATED, error.reason());
        assertEquals(4, error.position());
    }

    @Test
    void previewIsLimitedAndMarkedTruncated() throws IOException {
        Path file = Files.writeString(directory.resolve("p.txt"), "A".repeat(100), StandardCharsets.US_ASCII);

        TextPreview preview = files.readPreview(file, 10);
        TextPreview full = files.readPreview(file, 100);

        assertEquals("A".repeat(10), preview.text());
        assertTrue(preview.truncated());
        assertFalse(full.truncated());
    }

    @Test
    void nonImageHasNoSize() throws IOException {
        Path file = Files.writeString(directory.resolve("x.bin"), "bukan gambar");

        assertTrue(files.imageSize(file).isEmpty());
    }

    @Test
    void missingFileBecomesDomainError() {
        Path missing = directory.resolve("hilang.txt");

        EncoderIoException error = assertThrows(EncoderIoException.class, () -> files.size(missing));

        assertEquals(missing, error.file());
    }

    private static byte[] pngBytes(int width, int height) throws IOException {
        Path temp = Files.createTempFile("img", ".png");
        try {
            ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", temp.toFile());
            return Files.readAllBytes(temp);
        } finally {
            Files.delete(temp);
        }
    }
}
