package io.github.lukmnh.toolbox.devutils.jwt.application;

import io.github.lukmnh.toolbox.devutils.jwt.domain.DecodedJwt;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JwtAlgorithm;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JwtSignatureVerifier;
import io.github.lukmnh.toolbox.devutils.jwt.domain.KeyMismatchException;
import io.github.lukmnh.toolbox.devutils.jwt.domain.KeyType;
import io.github.lukmnh.toolbox.devutils.jwt.domain.PublicKeyParser;
import io.github.lukmnh.toolbox.devutils.jwt.domain.SignatureStatus;
import io.github.lukmnh.toolbox.devutils.jwt.domain.UnreadableKeyException;
import io.github.lukmnh.toolbox.devutils.jwt.domain.UnsecuredJwtException;
import io.github.lukmnh.toolbox.devutils.jwt.domain.UnsupportedAlgorithmException;
import io.github.lukmnh.toolbox.devutils.jwt.domain.VerificationKey;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/** Verifikasi signature token yang sudah di-decode. Secret selalu di-wipe, termasuk saat gagal (Security #7). */
public final class VerifyJwtSignatureUseCase {

    private static final char[] PEM_MARKER = "-----BEGIN".toCharArray();
    private static final char[] JWK_MARKER = "\"kty\"".toCharArray();
    private static final char ASCII_LIMIT = 0x7f;

    private final PublicKeyParser keyParser;
    private final JwtSignatureVerifier verifier;

    public VerifyJwtSignatureUseCase(PublicKeyParser keyParser, JwtSignatureVerifier verifier) {
        this.keyParser = keyParser;
        this.verifier = verifier;
    }

    /**
     * @throws UnsupportedAlgorithmException bila {@code alg} tidak ada atau tidak dikenal
     * @throws UnsecuredJwtException untuk {@code alg: none}
     * @throws KeyMismatchException bila jenis key tidak cocok dengan {@code alg}
     * @throws UnreadableKeyException bila key/secret tidak bisa dibaca
     */
    public SignatureStatus execute(DecodedJwt jwt, KeyInput input) {
        try {
            JwtAlgorithm algorithm =
                    jwt.algorithm().orElseThrow(() -> new UnsupportedAlgorithmException(jwt.algorithmName()));
            // Ditolak sebelum key disentuh: token tanpa signature tidak butuh (dan tidak boleh memproses) key apa pun
            if (algorithm == JwtAlgorithm.NONE) {
                throw new UnsecuredJwtException();
            }
            boolean valid = switch (input) {
                case KeyInput.Secret secret -> verifyWithSecret(jwt, algorithm, secret);
                case KeyInput.PublicKeyText text ->
                    verifier.verify(jwt.raw(), algorithm, new VerificationKey.Public(keyParser.parse(text.text())));
            };
            return valid ? SignatureStatus.VALID : SignatureStatus.INVALID;
        } finally {
            if (input instanceof KeyInput.Secret secret) {
                Arrays.fill(secret.chars(), '\0');
            }
        }
    }

    private boolean verifyWithSecret(DecodedJwt jwt, JwtAlgorithm algorithm, KeyInput.Secret secret) {
        char[] chars = secret.chars();
        VerificationKey.HmacSecret key = null;
        try {
            // Teks PEM/JWK di kolom secret adalah pola algorithm confusion; tolak sebelum dipakai sebagai HMAC key
            if (contains(chars, PEM_MARKER) || contains(chars, JWK_MARKER)) {
                throw new KeyMismatchException(algorithm, KeyType.PUBLIC_KEY_AS_SECRET);
            }
            key = new VerificationKey.HmacSecret(secret.base64() ? decodeBase64(chars) : utf8(chars));
            return verifier.verify(jwt.raw(), algorithm, key);
        } finally {
            if (key != null) {
                key.wipe();
            }
        }
    }

    /** Encode tanpa membuat {@link String} yang tidak bisa di-wipe. */
    private static byte[] utf8(char[] chars) {
        ByteBuffer encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars));
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        if (encoded.hasArray()) {
            Arrays.fill(encoded.array(), (byte) 0);
        }
        return bytes;
    }

    /** Terima alfabet Base64 biasa maupun URL-safe, dengan atau tanpa padding, dan abaikan whitespace. */
    private static byte[] decodeBase64(char[] chars) {
        byte[] ascii = new byte[chars.length];
        byte[] trimmed = new byte[0];
        int length = 0;
        try {
            for (char current : chars) {
                if (Character.isWhitespace(current)) {
                    continue;
                }
                if (current > ASCII_LIMIT) {
                    throw new UnreadableKeyException(UnreadableKeyException.Reason.INVALID_BASE64_SECRET);
                }
                ascii[length++] = (byte) toStandardAlphabet(current);
            }
            trimmed = Arrays.copyOf(ascii, length);
            return Base64.getDecoder().decode(trimmed);
        } catch (IllegalArgumentException e) {
            throw new UnreadableKeyException(UnreadableKeyException.Reason.INVALID_BASE64_SECRET, e);
        } finally {
            Arrays.fill(ascii, (byte) 0);
            Arrays.fill(trimmed, (byte) 0);
        }
    }

    private static char toStandardAlphabet(char current) {
        return switch (current) {
            case '-' -> '+';
            case '_' -> '/';
            default -> current;
        };
    }

    private static boolean contains(char[] haystack, char[] needle) {
        for (int start = 0; start + needle.length <= haystack.length; start++) {
            if (regionMatches(haystack, start, needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean regionMatches(char[] haystack, int start, char[] needle) {
        for (int index = 0; index < needle.length; index++) {
            if (haystack[start + index] != needle[index]) {
                return false;
            }
        }
        return true;
    }
}
