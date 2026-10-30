package io.github.lukmnh.toolbox.devutils.hash.domain;

import java.util.Objects;

/**
 * Key pair hasil generate dalam format PEM.
 *
 * @param publicKeyPem SPKI ({@code -----BEGIN PUBLIC KEY-----})
 * @param privateKeyPem PKCS#8 tanpa enkripsi ({@code -----BEGIN PRIVATE KEY-----})
 */
public record PemKeyPair(KeyPairAlgorithm algorithm, String publicKeyPem, String privateKeyPem) {

    public PemKeyPair {
        Objects.requireNonNull(algorithm, "algorithm");
        Objects.requireNonNull(publicKeyPem, "publicKeyPem");
        Objects.requireNonNull(privateKeyPem, "privateKeyPem");
    }

    @Override
    public String toString() {
        // Private key tidak boleh ikut ke log (Security Requirements #5)
        return "PemKeyPair[" + algorithm.label() + "]";
    }
}
