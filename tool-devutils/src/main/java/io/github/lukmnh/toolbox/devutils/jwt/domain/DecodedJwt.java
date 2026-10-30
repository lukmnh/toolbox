package io.github.lukmnh.toolbox.devutils.jwt.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Hasil decode lengkap untuk ditampilkan.
 *
 * @param algorithmName nilai {@code alg} di header; kosong bila tidak ada
 * @param claims field top-level payload sesuai urutan di token
 */
public record DecodedJwt(
        RawJwt raw, String prettyHeader, String prettyPayload, String algorithmName, List<Claim> claims) {

    public DecodedJwt {
        Objects.requireNonNull(raw, "raw");
        Objects.requireNonNull(prettyHeader, "prettyHeader");
        Objects.requireNonNull(prettyPayload, "prettyPayload");
        Objects.requireNonNull(algorithmName, "algorithmName");
        claims = List.copyOf(claims);
    }

    /** Kosong bila {@code alg} tidak ada atau tidak dikenal. */
    public Optional<JwtAlgorithm> algorithm() {
        return JwtAlgorithm.fromHeader(algorithmName);
    }

    public TimeStatus timeStatus(Instant now, Duration clockSkew) {
        return TimeValidity.classify(claims, now, clockSkew);
    }
}
