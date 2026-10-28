package io.github.lukmnh.toolbox.pdf.sign.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfLimitExceededException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class FileSignatureRepositoryTest {

    private static final int BOMB_DIMENSION = 50_000;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    @TempDir
    Path temp;

    @Test
    void decompressionBombIsRejectedFromHeaderBeforeDecode() throws IOException {
        Path bomb = Files.write(temp.resolve("bom.png"), pngHeaderOnly(BOMB_DIMENSION, BOMB_DIMENSION));
        FileSignatureRepository repository = new FileSignatureRepository(temp);

        PdfLimitExceededException error =
                assertThrows(PdfLimitExceededException.class, () -> repository.readImage(bomb));

        assertEquals(PdfLimitExceededException.Limit.IMAGE_DIMENSION, error.limit());
        assertEquals(BOMB_DIMENSION, error.actual());
        assertEquals(SecurityLimits.IMAGE_MAX_DIMENSION, error.maximum());
    }

    @Test
    void normalImageIsDecoded() throws IOException {
        Path image = temp.resolve("ttd.png");
        ImageIO.write(new BufferedImage(30, 20, BufferedImage.TYPE_INT_ARGB), "png", image.toFile());

        BufferedImage read = new FileSignatureRepository(temp).readImage(image);

        assertEquals(30, read.getWidth());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void savedSignatureIsOwnerOnly() throws IOException {
        FileSignatureRepository repository = new FileSignatureRepository(temp);

        repository.saveLast(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB));

        Path saved = temp.resolve("signature.png");
        assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(saved)));
        assertTrue(repository.loadLast().isPresent());
    }

    @Test
    void deleteLastRemovesSavedSignature() {
        FileSignatureRepository repository = new FileSignatureRepository(temp);
        repository.saveLast(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB));

        repository.deleteLast();
        repository.deleteLast();

        assertFalse(Files.exists(temp.resolve("signature.png")));
        assertTrue(repository.loadLast().isEmpty());
    }

    /** PNG berisi signature + IHDR + IEND saja: header valid, tanpa data pixel. */
    private static byte[] pngHeaderOnly(int width, int height) throws IOException {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        png.write(PNG_SIGNATURE);
        ByteBuffer header = ByteBuffer.allocate(13).putInt(width).putInt(height);
        // bit depth 8, color type 6 (RGBA), compression, filter, interlace
        header.put((byte) 8).put((byte) 6).put((byte) 0).put((byte) 0).put((byte) 0);
        writeChunk(png, "IHDR", header.array());
        writeChunk(png, "IEND", new byte[0]);
        return png.toByteArray();
    }

    private static void writeChunk(ByteArrayOutputStream out, String type, byte[] data) throws IOException {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        out.write(ByteBuffer.allocate(4).putInt(data.length).array());
        out.write(typeBytes);
        out.write(data);
        out.write(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }
}
