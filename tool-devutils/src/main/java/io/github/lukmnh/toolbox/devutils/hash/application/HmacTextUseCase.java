package io.github.lukmnh.toolbox.devutils.hash.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.devutils.hash.domain.DigestResult;
import io.github.lukmnh.toolbox.devutils.hash.domain.HmacAlgorithm;
import io.github.lukmnh.toolbox.devutils.hash.domain.TextHasher;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * HMAC satu teks (UTF-8) dengan semua {@link HmacAlgorithm}. Key tidak pernah dijadikan {@link String} dan
 * di-wipe setelah dipakai (Security Requirements #7).
 */
public final class HmacTextUseCase {

    private final TextHasher hasher;

    public HmacTextUseCase(TextHasher hasher) {
        this.hasher = hasher;
    }

    /**
     * @param key secret dari {@code PasswordField}; selalu dikosongkan ({@code '\0'}) sebelum method kembali,
     *     termasuk saat gagal
     * @throws IllegalArgumentException bila key kosong atau berisi surrogate UTF-16 yang tidak valid
     */
    public List<DigestResult> execute(String text, char[] key, CancellationToken cancellation) {
        byte[] keyBytes = new byte[0];
        try {
            keyBytes = utf8(key);
            byte[] input = text.getBytes(StandardCharsets.UTF_8);
            List<DigestResult> results = new ArrayList<>();
            for (HmacAlgorithm algorithm : HmacAlgorithm.values()) {
                cancellation.throwIfCancelled();
                results.add(hasher.hmac(algorithm, keyBytes, input));
            }
            return List.copyOf(results);
        } finally {
            Arrays.fill(key, '\0');
            Arrays.fill(keyBytes, (byte) 0);
        }
    }

    /** Encode tanpa membuat {@link String}; buffer perantara ikut di-wipe. */
    private static byte[] utf8(char[] key) {
        ByteBuffer encoded;
        try {
            encoded = StandardCharsets.UTF_8
                    .newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(key));
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Secret key berisi karakter yang tidak valid", e);
        }
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        if (encoded.hasArray()) {
            Arrays.fill(encoded.array(), (byte) 0);
        }
        return bytes;
    }
}
