package io.github.lukmnh.toolbox.devutils.hash.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.devutils.hash.domain.KeyPairAlgorithm;
import io.github.lukmnh.toolbox.devutils.hash.domain.Pem;
import io.github.lukmnh.toolbox.devutils.hash.domain.PemKeyPair;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Arrays;

/**
 * Generate key pair baru lalu ekspor ke PEM: public key SPKI, private key PKCS#8 tanpa enkripsi. RSA 4096 bisa
 * makan beberapa detik, jadi dipanggil dari background task.
 */
public final class GenerateKeyPairUseCase {

    private final SecureRandom random;

    public GenerateKeyPairUseCase(SecureRandom random) {
        this.random = random;
    }

    public PemKeyPair execute(KeyPairAlgorithm algorithm, CancellationToken cancellation) {
        KeyPair keyPair = generate(algorithm);
        // Generate tidak bisa diinterupsi; hasil yang sudah usang cukup dibuang
        cancellation.throwIfCancelled();
        byte[] privateDer = keyPair.getPrivate().getEncoded();
        try {
            return new PemKeyPair(
                    algorithm,
                    Pem.encode(Pem.PUBLIC_KEY, keyPair.getPublic().getEncoded()),
                    Pem.encode(Pem.PRIVATE_KEY, privateDer));
        } finally {
            Arrays.fill(privateDer, (byte) 0);
        }
    }

    private KeyPair generate(KeyPairAlgorithm algorithm) {
        try {
            KeyPairGenerator generator =
                    KeyPairGenerator.getInstance(algorithm.family().jcaName());
            switch (algorithm.family()) {
                case RSA ->
                    generator.initialize(
                            new RSAKeyGenParameterSpec(algorithm.bits(), RSAKeyGenParameterSpec.F4), random);
                case EC -> generator.initialize(new ECGenParameterSpec(algorithm.curveName()), random);
                case ED25519 -> generator.initialize(algorithm.bits(), random);
            }
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            // Semua algoritma di enum tersedia di JDK 21; kalau tidak, runtime-nya rusak
            throw new IllegalStateException("Algoritma " + algorithm.label() + " tidak tersedia di JDK", e);
        }
    }
}
