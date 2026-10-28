package io.github.lukmnh.toolbox.devutils.jwt.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Klasifikasi waktu token (RFC 7519 §4.1.4–4.1.5) dengan toleransi clock skew. */
public final class TimeValidity {

    public static final Duration DEFAULT_CLOCK_SKEW = Duration.ofSeconds(60);

    private TimeValidity() {}

    /**
     * @param clockSkew toleransi perbedaan jam antara penerbit dan pemeriksa; tidak boleh negatif
     */
    public static TimeStatus classify(List<Claim> claims, Instant now, Duration clockSkew) {
        if (clockSkew.isNegative()) {
            throw new IllegalArgumentException("Clock skew tidak boleh negatif");
        }
        Optional<Instant> expiresAt = instantOf(claims, "exp");
        Optional<Instant> notBefore = instantOf(claims, "nbf");
        if (expiresAt.isEmpty() && notBefore.isEmpty()) {
            return TimeStatus.NO_TIME_CLAIMS;
        }
        // Token sah selama now < exp (+skew) dan now >= nbf (-skew)
        if (expiresAt.isPresent() && !now.isBefore(expiresAt.get().plus(clockSkew))) {
            return TimeStatus.EXPIRED;
        }
        if (notBefore.isPresent() && now.isBefore(notBefore.get().minus(clockSkew))) {
            return TimeStatus.NOT_YET_VALID;
        }
        return TimeStatus.ACTIVE;
    }

    private static Optional<Instant> instantOf(List<Claim> claims, String name) {
        return claims.stream()
                .filter(claim -> claim.name().equals(name))
                .findFirst()
                .flatMap(Claim::instant);
    }
}
