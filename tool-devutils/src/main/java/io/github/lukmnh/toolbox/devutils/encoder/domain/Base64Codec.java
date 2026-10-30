package io.github.lukmnh.toolbox.devutils.encoder.domain;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Encode/decode Base64 untuk teks di memori (mode Teks dan Base64 hasil paste). */
public final class Base64Codec {

    private static final int QUANTUM = 4;
    private static final int MAX_PADDING = 2;

    private Base64Codec() {}

    public static TextConversion encode(String text, Base64Options options) {
        byte[] bytes = text.getBytes(options.charset().charset());
        String output = options.variant().encoder(options.padding()).encodeToString(bytes);
        return new TextConversion(output, text.length(), bytes.length, output.length(), output.length());
    }

    /** Decode lalu ubah ke teks dengan charset pilihan; byte yang bukan teks valid tampil sebagai karakter pengganti. */
    public static TextConversion decode(String input, Base64Options options) {
        byte[] bytes = decodeBytes(input);
        String output = new String(bytes, options.charset().charset());
        long inputBytes = input.getBytes(StandardCharsets.UTF_8).length;
        return new TextConversion(output, input.length(), inputBytes, output.length(), bytes.length);
    }

    /**
     * Decode toleran: whitespace dan prefix data URI diabaikan, alfabet standar maupun URL-safe diterima, padding
     * opsional.
     *
     * @throws InvalidBase64Exception dengan posisi terhadap {@code input} asli
     */
    public static byte[] decodeBytes(String input) {
        StringBuilder data = new StringBuilder(input.length());
        int padding = 0;
        int lastDataIndex = -1;
        for (int index = DataUri.payloadStart(input); index < input.length(); index++) {
            char character = input.charAt(index);
            if (Base64Alphabet.isWhitespace(character)) {
                continue;
            }
            if (character == Base64Alphabet.PADDING) {
                padding++;
                requirePadding(padding <= MAX_PADDING, index, character);
                continue;
            }
            requirePadding(padding == 0, index, input.codePointAt(index));
            int normalized = Base64Alphabet.normalize(character);
            if (normalized < 0) {
                throw new InvalidBase64Exception(
                        InvalidBase64Exception.Reason.INVALID_CHARACTER, index, input.codePointAt(index));
            }
            data.append((char) normalized);
            lastDataIndex = index;
        }
        if (data.length() % QUANTUM == 1) {
            throw new InvalidBase64Exception(InvalidBase64Exception.Reason.TRUNCATED, lastDataIndex, -1);
        }
        // Decoder JDK menerima unit akhir tanpa padding, jadi padding user (benar/salah jumlahnya) cukup dibuang
        return Base64.getDecoder().decode(data.toString());
    }

    private static void requirePadding(boolean valid, int index, int character) {
        if (!valid) {
            throw new InvalidBase64Exception(InvalidBase64Exception.Reason.MISPLACED_PADDING, index, character);
        }
    }
}
