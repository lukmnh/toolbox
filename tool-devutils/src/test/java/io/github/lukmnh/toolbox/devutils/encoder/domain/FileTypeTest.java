package io.github.lukmnh.toolbox.devutils.encoder.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class FileTypeTest {

    @Test
    void detectsKnownSignatures() {
        assertEquals(FileType.PNG, FileType.detect(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}));
        assertEquals(FileType.JPEG, FileType.detect(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}));
        assertEquals(FileType.GIF, FileType.detect(ascii("GIF89a")));
        assertEquals(FileType.WEBP, FileType.detect(ascii("RIFF\0\0\0\0WEBPVP8 ")));
        assertEquals(FileType.PDF, FileType.detect(ascii("%PDF-1.7")));
        assertEquals(FileType.ZIP, FileType.detect(new byte[] {'P', 'K', 3, 4, 20}));
        assertEquals(FileType.GZIP, FileType.detect(new byte[] {0x1F, (byte) 0x8B, 8}));
    }

    @Test
    void unknownOrShortHeaderIsBinary() {
        assertEquals(FileType.BINARY, FileType.detect(new byte[0]));
        assertEquals(FileType.BINARY, FileType.detect(ascii("RIFF\0\0\0\0WAVE")));
        assertEquals(FileType.BINARY, FileType.detect(ascii("hello")));
        assertEquals("bin", FileType.BINARY.extension());
    }

    @Test
    void onlyJavaFxImagesArePreviewable() {
        assertTrue(FileType.PNG.previewable());
        assertFalse(FileType.WEBP.previewable());
        assertFalse(FileType.PDF.previewable());
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }
}
