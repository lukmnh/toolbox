package io.github.lukmnh.toolbox.devutils.hash.application;

import io.github.lukmnh.toolbox.devutils.hash.domain.SecretEncoding;
import java.util.Arrays;
import java.util.List;
import java.util.random.RandomGenerator;

/** Random secret (API key, signing secret, salt) dari CSPRNG, langsung di-encode. */
public final class GenerateSecretUseCase {

    /** Pilihan panjang di UI: 128, 256, 384, 512 bit. */
    public static final List<Integer> BYTE_LENGTHS = List.of(16, 32, 48, 64);

    static final int MAX_BYTES = 1024;

    private final RandomGenerator random;

    /** @param random produksi wajib {@code SecureRandom}; parameter ini supaya test deterministik */
    public GenerateSecretUseCase(RandomGenerator random) {
        this.random = random;
    }

    /** @throws IllegalArgumentException bila {@code byteLength} di luar 1–{@value #MAX_BYTES} */
    public String execute(int byteLength, SecretEncoding encoding) {
        if (byteLength < 1 || byteLength > MAX_BYTES) {
            throw new IllegalArgumentException("Secret length must be 1–" + MAX_BYTES + " bytes: " + byteLength);
        }
        byte[] bytes = new byte[byteLength];
        try {
            random.nextBytes(bytes);
            return encoding.encode(bytes);
        } finally {
            // Byte mentah tidak perlu tinggal di heap; String hasil memang harus ditampilkan
            Arrays.fill(bytes, (byte) 0);
        }
    }
}
