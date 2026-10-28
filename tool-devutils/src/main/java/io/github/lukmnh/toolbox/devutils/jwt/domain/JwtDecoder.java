package io.github.lukmnh.toolbox.devutils.jwt.domain;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

/** Memisah dan men-decode token JWS kompak ({@code header.payload.signature}) tanpa memverifikasi apa pun. */
public final class JwtDecoder {

    private static final String BEARER_PREFIX = "bearer ";
    private static final int JWS_PART_COUNT = 3;
    private static final int JWE_PART_COUNT = 5;

    private JwtDecoder() {}

    /**
     * @throws JweNotSupportedException bila token terdiri dari 5 bagian
     * @throws MalformedJwtException bila struktur atau Base64URL salah, menunjuk bagian yang rusak
     */
    public static RawJwt decode(String token) {
        String compact = normalize(token);
        if (compact.isEmpty()) {
            throw new MalformedJwtException(JwtPart.TOKEN, MalformedJwtException.Reason.EMPTY_TOKEN);
        }
        String[] parts = compact.split("\\.", -1);
        if (parts.length == JWE_PART_COUNT) {
            throw new JweNotSupportedException();
        }
        if (parts.length != JWS_PART_COUNT) {
            throw new MalformedJwtException(
                    JwtPart.TOKEN, MalformedJwtException.Reason.WRONG_PART_COUNT, String.valueOf(parts.length));
        }
        String header = decodeText(parts[0], JwtPart.HEADER);
        String payload = decodeText(parts[1], JwtPart.PAYLOAD);
        byte[] signature = decodeBytes(parts[2], JwtPart.SIGNATURE, true);
        byte[] signingInput = (parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII);
        return new RawJwt(header, payload, signature, parts[2], signingInput);
    }

    /**
     * Buang prefix {@code Bearer } (dari header HTTP yang di-copy) dan semua whitespace. Token kompak tidak pernah
     * berisi whitespace, jadi baris baru hasil copy dari log aman dibuang.
     */
    public static String normalize(String token) {
        String trimmed = token.strip();
        if (trimmed.toLowerCase(Locale.ROOT).startsWith(BEARER_PREFIX)) {
            trimmed = trimmed.substring(BEARER_PREFIX.length());
        }
        return trimmed.replaceAll("\\s+", "");
    }

    private static String decodeText(String segment, JwtPart part) {
        byte[] bytes = decodeBytes(segment, part, false);
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new MalformedJwtException(part, MalformedJwtException.Reason.NOT_UTF8);
        }
    }

    private static byte[] decodeBytes(String segment, JwtPart part, boolean emptyAllowed) {
        if (segment.isEmpty() && !emptyAllowed) {
            throw new MalformedJwtException(part, MalformedJwtException.Reason.EMPTY_PART);
        }
        try {
            // Decoder URL JDK tidak mewajibkan padding, tapi menolak '+' dan '/' dari alfabet Base64 biasa
            return Base64.getUrlDecoder().decode(segment);
        } catch (IllegalArgumentException e) {
            throw new MalformedJwtException(part, MalformedJwtException.Reason.INVALID_BASE64URL);
        }
    }
}
