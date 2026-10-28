package io.github.lukmnh.toolbox.devutils.jwt.domain;

import java.io.Serial;
import java.util.Objects;

/**
 * Jenis key tidak cocok dengan {@code alg} di header. Verifikasi ditolak, bukan dicoba, untuk mencegah algorithm
 * confusion (misalnya header {@code HS256} + public key RSA dipakai sebagai secret HMAC; Security #20).
 */
public final class KeyMismatchException extends JwtToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final JwtAlgorithm algorithm;
    private final KeyType keyType;
    private final int curveBits;

    /** @param curveBits ukuran kurva EC key yang diberikan; 0 bila bukan EC */
    public KeyMismatchException(JwtAlgorithm algorithm, KeyType keyType, int curveBits) {
        super("Key " + keyType + " tidak cocok dengan " + algorithm.headerName());
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm");
        this.keyType = Objects.requireNonNull(keyType, "keyType");
        this.curveBits = curveBits;
    }

    public KeyMismatchException(JwtAlgorithm algorithm, KeyType keyType) {
        this(algorithm, keyType, 0);
    }

    public JwtAlgorithm algorithm() {
        return algorithm;
    }

    public KeyType keyType() {
        return keyType;
    }

    public int curveBits() {
        return curveBits;
    }
}
