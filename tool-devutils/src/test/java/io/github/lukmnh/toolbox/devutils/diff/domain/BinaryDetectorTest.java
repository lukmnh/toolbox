package io.github.lukmnh.toolbox.devutils.diff.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class BinaryDetectorTest {

    @Test
    void textIsNotBinary() {
        byte[] text = "halo\nдобрый день ✓\n".getBytes(StandardCharsets.UTF_8);
        assertFalse(BinaryDetector.isBinary(text, text.length));
    }

    @Test
    void nulByteInFirst8KbIsBinary() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
        assertTrue(BinaryDetector.isBinary(png, png.length));
    }

    @Test
    void nulByteAfter8KbIsIgnored() {
        byte[] data = new byte[BinaryDetector.SNIFF_BYTES + 10];
        java.util.Arrays.fill(data, (byte) 'a');
        data[BinaryDetector.SNIFF_BYTES + 5] = 0;
        assertFalse(BinaryDetector.isBinary(data, data.length));
    }
}
