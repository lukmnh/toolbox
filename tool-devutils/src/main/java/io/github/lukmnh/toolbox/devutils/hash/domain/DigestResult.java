package io.github.lukmnh.toolbox.devutils.hash.domain;

import java.util.Arrays;
import java.util.Objects;

/**
 * Hasil satu algoritma. Digest disimpan mentah supaya ganti format (hex/Base64) tidak perlu hitung ulang.
 *
 * @param algorithm nama algoritma untuk ditampilkan, misalnya {@code SHA-256}
 * @param insecure {@code true} bila algoritma tidak aman untuk keperluan keamanan
 */
public record DigestResult(String algorithm, byte[] digest, boolean insecure) {

    public DigestResult {
        Objects.requireNonNull(algorithm, "algorithm");
        digest = Objects.requireNonNull(digest, "digest").clone();
    }

    @Override
    public byte[] digest() {
        return digest.clone();
    }

    public String encoded(DigestEncoding encoding) {
        return encoding.encode(digest);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DigestResult that
                && algorithm.equals(that.algorithm)
                && insecure == that.insecure
                && Arrays.equals(digest, that.digest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(algorithm, insecure, Arrays.hashCode(digest));
    }

    @Override
    public String toString() {
        // Digest dari input user tidak boleh ikut ke log (Security Requirements #5)
        return "DigestResult[" + algorithm + "]";
    }
}
