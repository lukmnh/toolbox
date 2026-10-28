package io.github.lukmnh.toolbox.devutils.hash.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class DigestEncodingTest {

    private static final byte[] BYTES = {(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef, 0x01};

    @Test
    void encodesHexLowerUpperAndBase64() {
        assertEquals("deadbeef01", DigestEncoding.HEX_LOWER.encode(BYTES));
        assertEquals("DEADBEEF01", DigestEncoding.HEX_UPPER.encode(BYTES));
        assertEquals("3q2+7wE=", DigestEncoding.BASE64.encode(BYTES));
    }

    @Test
    void resultsWithSameDigestAreEqual() {
        assertEquals(new DigestResult("MD5", BYTES, true), new DigestResult("MD5", BYTES.clone(), true));
        assertNotEquals(new DigestResult("MD5", BYTES, true), new DigestResult("MD5", new byte[] {1}, true));
    }
}
