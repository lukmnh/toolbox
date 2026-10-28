package io.github.lukmnh.toolbox.devutils.encoder.domain;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/** URL encode/decode UTF-8 untuk komponen (key, value, segmen path) dan URL lengkap. */
public final class UrlCodec {

    /** Unreserved + reserved RFC 3986: struktur URL yang harus dipertahankan saat encode URL lengkap. */
    private static final String URL_SAFE_CHARACTERS = "-._~:/?#[]@!$&'()*+,;=";

    private static final int PERCENT_SEQUENCE_LENGTH = 3;
    private static final HexFormat UPPER_HEX = HexFormat.of().withUpperCase();

    private UrlCodec() {}

    public static String encodeComponent(String text, SpaceEncoding spaces) {
        // URLEncoder memakai aturan form (spasi → '+', '~' → %7E); dirapikan agar sesuai RFC 3986
        String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8).replace("%7E", "~");
        return spaces == SpaceEncoding.PERCENT_20 ? encoded.replace("+", "%20") : encoded;
    }

    /**
     * Decode toleran: karakter non-ASCII yang sudah ter-decode dibiarkan, byte UTF-8 rusak menjadi karakter pengganti.
     *
     * @param plusAsSpace {@code true} untuk query string/form, di mana {@code +} berarti spasi
     * @throws InvalidPercentEncodingException bila {@code %} tidak diikuti dua digit hex
     */
    public static String decodeComponent(String text, boolean plusAsSpace) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(text.length());
        int index = 0;
        while (index < text.length()) {
            char character = text.charAt(index);
            if (character == '%') {
                bytes.write(percentByte(text, index));
                index += PERCENT_SEQUENCE_LENGTH;
                continue;
            }
            if (character == '+' && plusAsSpace) {
                bytes.write(' ');
                index++;
                continue;
            }
            int codePoint = text.codePointAt(index);
            bytes.writeBytes(Character.toString(codePoint).getBytes(StandardCharsets.UTF_8));
            index += Character.charCount(codePoint);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    /** Encode hanya karakter yang ilegal di URL; struktur {@code :/?#&=} dan {@code %XX} yang sudah ada dipertahankan. */
    public static String encodeFullUrl(String url) {
        StringBuilder encoded = new StringBuilder(url.length());
        int index = 0;
        while (index < url.length()) {
            int codePoint = url.codePointAt(index);
            if (isAllowedAsIs(url, index, codePoint)) {
                encoded.appendCodePoint(codePoint);
            } else {
                appendPercentEncoded(encoded, codePoint);
            }
            index += Character.charCount(codePoint);
        }
        return encoded.toString();
    }

    private static boolean isAllowedAsIs(String url, int index, int codePoint) {
        if (codePoint == '%') {
            return hasHexPair(url, index);
        }
        boolean alphanumeric = (codePoint >= 'a' && codePoint <= 'z')
                || (codePoint >= 'A' && codePoint <= 'Z')
                || (codePoint >= '0' && codePoint <= '9');
        return alphanumeric || (codePoint < 0x80 && URL_SAFE_CHARACTERS.indexOf(codePoint) >= 0);
    }

    private static void appendPercentEncoded(StringBuilder target, int codePoint) {
        for (byte value : Character.toString(codePoint).getBytes(StandardCharsets.UTF_8)) {
            target.append('%').append(UPPER_HEX.toHexDigits(value));
        }
    }

    private static int percentByte(String text, int index) {
        if (!hasHexPair(text, index)) {
            int end = Math.min(text.length(), index + PERCENT_SEQUENCE_LENGTH);
            throw new InvalidPercentEncodingException(index, text.substring(index, end));
        }
        return HexFormat.fromHexDigits(text, index + 1, index + PERCENT_SEQUENCE_LENGTH);
    }

    private static boolean hasHexPair(String text, int index) {
        return index + 2 < text.length()
                && HexFormat.isHexDigit(text.charAt(index + 1))
                && HexFormat.isHexDigit(text.charAt(index + 2));
    }
}
