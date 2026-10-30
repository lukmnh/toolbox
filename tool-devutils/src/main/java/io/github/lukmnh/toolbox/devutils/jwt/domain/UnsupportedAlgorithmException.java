package io.github.lukmnh.toolbox.devutils.jwt.domain;

import java.io.Serial;
import java.util.Objects;

/** Header tidak punya {@code alg}, atau nilainya bukan algoritma JWS yang didukung. */
public final class UnsupportedAlgorithmException extends JwtToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String algorithmName;

    /** @param algorithmName nilai {@code alg} di header; kosong bila tidak ada */
    public UnsupportedAlgorithmException(String algorithmName) {
        super("Unsupported algorithm: " + algorithmName);
        this.algorithmName = Objects.requireNonNull(algorithmName, "algorithmName");
    }

    public String algorithmName() {
        return algorithmName;
    }
}
