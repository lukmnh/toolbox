package io.github.lukmnh.toolbox.devutils.jwt.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TimeValidityTest {

    private static final Instant NOW = Instant.parse("2026-11-02T01:00:00Z");
    private static final Duration SKEW = Duration.ofSeconds(60);

    @Test
    void withoutExpAndNbfThereIsNoTimeStatus() {
        assertEquals(TimeStatus.NO_TIME_CLAIMS, classify(List.of(number("iat", NOW.getEpochSecond()))));
    }

    @Test
    void expiredTokenOutsideSkew() {
        assertEquals(
                TimeStatus.EXPIRED,
                classify(List.of(number("exp", NOW.minusSeconds(61).getEpochSecond()))));
        assertEquals(
                TimeStatus.EXPIRED,
                classify(List.of(number("exp", NOW.minusSeconds(60).getEpochSecond()))));
    }

    @Test
    void recentlyExpiredTokenIsActiveWithinSkew() {
        assertEquals(
                TimeStatus.ACTIVE,
                classify(List.of(number("exp", NOW.minusSeconds(59).getEpochSecond()))));
        assertEquals(
                TimeStatus.EXPIRED,
                TimeValidity.classify(
                        List.of(number("exp", NOW.minusSeconds(59).getEpochSecond())), NOW, Duration.ZERO));
    }

    @Test
    void notYetValidTokenOutsideSkew() {
        assertEquals(
                TimeStatus.NOT_YET_VALID,
                classify(List.of(number("nbf", NOW.plusSeconds(61).getEpochSecond()))));
        assertEquals(
                TimeStatus.ACTIVE,
                classify(List.of(number("nbf", NOW.plusSeconds(60).getEpochSecond()))));
    }

    @Test
    void activeBetweenNbfAndExp() {
        assertEquals(
                TimeStatus.ACTIVE,
                classify(List.of(
                        number("nbf", NOW.minusSeconds(3_600).getEpochSecond()),
                        number("exp", NOW.plusSeconds(3_600).getEpochSecond()))));
    }

    @Test
    void nonNumericOrHugeTimeClaimsAreIgnored() {
        Claim text = new Claim("exp", "besok", Claim.ValueType.STRING);
        Claim huge = new Claim("exp", "1e300", Claim.ValueType.NUMBER);
        Claim fraction = new Claim("exp", "1300819380.9", Claim.ValueType.NUMBER);

        assertEquals(TimeStatus.NO_TIME_CLAIMS, classify(List.of(text)));
        assertTrue(huge.instant().isEmpty());
        assertEquals(Instant.ofEpochSecond(1_300_819_380), fraction.instant().orElseThrow());
    }

    @Test
    void negativeSkewIsRejected() {
        assertThrows(
                IllegalArgumentException.class, () -> TimeValidity.classify(List.of(), NOW, Duration.ofSeconds(-1)));
    }

    private static TimeStatus classify(List<Claim> claims) {
        return TimeValidity.classify(claims, NOW, SKEW);
    }

    private static Claim number(String name, long epochSeconds) {
        return new Claim(name, String.valueOf(epochSeconds), Claim.ValueType.NUMBER);
    }
}
