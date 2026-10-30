package io.github.lukmnh.toolbox.json.viewer.domain;

import java.nio.charset.StandardCharsets;

/**
 * Quick search langsung di byte mentah record, tanpa parse JSON. Tidak peka huruf besar/kecil untuk huruf
 * ASCII saja; karakter lain harus sama persis (termasuk bentuk escape {@code \\uXXXX} di file).
 */
public final class ByteSearch {

    private static final int ASCII_CASE_OFFSET = 'a' - 'A';

    private ByteSearch() {}

    /** Ubah teks pencarian ke byte UTF-8 dengan huruf ASCII kecil, sekali sebelum memindai banyak record. */
    public static byte[] needle(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = lowerAscii(bytes[i]);
        }
        return bytes;
    }

    /** @param needle hasil {@link #needle(String)} */
    public static boolean contains(byte[] haystack, byte[] needle) {
        if (needle.length == 0) {
            return true;
        }
        int last = haystack.length - needle.length;
        for (int start = 0; start <= last; start++) {
            if (matchesAt(haystack, start, needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAt(byte[] haystack, int start, byte[] needle) {
        for (int i = 0; i < needle.length; i++) {
            if (lowerAscii(haystack[start + i]) != needle[i]) {
                return false;
            }
        }
        return true;
    }

    private static byte lowerAscii(byte value) {
        return value >= 'A' && value <= 'Z' ? (byte) (value + ASCII_CASE_OFFSET) : value;
    }
}
