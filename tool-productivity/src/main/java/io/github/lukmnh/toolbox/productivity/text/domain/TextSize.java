package io.github.lukmnh.toolbox.productivity.text.domain;

/** Ukuran teks dalam byte UTF-8, dihitung tanpa meng-encode (tanpa alokasi array sebesar teks). */
public final class TextSize {

    /** Satu char UTF-16 paling banyak 3 byte UTF-8 (pasangan surrogate = 2 char → 4 byte). */
    private static final int MAX_UTF8_BYTES_PER_CHAR = 3;

    private static final int ONE_BYTE_LIMIT = 0x80;
    private static final int TWO_BYTE_LIMIT = 0x800;
    private static final int SURROGATE_PAIR_BYTES = 4;

    private TextSize() {}

    public static long utf8Bytes(CharSequence text) {
        long bytes = 0;
        int length = text.length();
        for (int index = 0; index < length; index++) {
            char character = text.charAt(index);
            if (character < ONE_BYTE_LIMIT) {
                bytes += 1;
            } else if (character < TWO_BYTE_LIMIT) {
                bytes += 2;
            } else if (Character.isHighSurrogate(character)
                    && index + 1 < length
                    && Character.isLowSurrogate(text.charAt(index + 1))) {
                bytes += SURROGATE_PAIR_BYTES;
                index++;
            } else {
                bytes += MAX_UTF8_BYTES_PER_CHAR;
            }
        }
        return bytes;
    }

    /** Jalur cepat: teks pendek pasti di bawah batas tanpa perlu dihitung per char (dipanggil tiap ketikan). */
    public static boolean exceeds(CharSequence text, long maxBytes) {
        if ((long) text.length() * MAX_UTF8_BYTES_PER_CHAR <= maxBytes) {
            return false;
        }
        return utf8Bytes(text) > maxBytes;
    }
}
