package io.github.lukmnh.toolbox.json.shared.domain;

import java.nio.charset.StandardCharsets;

/** Decode potongan byte UTF-8 tanpa memotong karakter multi-byte di tengah. */
public final class Utf8Text {

    private static final int CONTINUATION_MASK = 0xC0;
    private static final int CONTINUATION_BITS = 0x80;

    private Utf8Text() {}

    public static boolean isContinuationByte(byte value) {
        return (value & CONTINUATION_MASK) == CONTINUATION_BITS;
    }

    /**
     * Decode maksimal {@code maxBytes} pertama. Bila batas jatuh di tengah karakter, karakter itu tidak
     * diikutkan.
     */
    public static String decodePrefix(byte[] bytes, int maxBytes) {
        if (bytes.length <= maxBytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        int end = maxBytes;
        while (end > 0 && isContinuationByte(bytes[end])) {
            end--;
        }
        return new String(bytes, 0, end, StandardCharsets.UTF_8);
    }
}
