package io.github.lukmnh.toolbox.devutils.hash.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.TaskCancelledException;
import io.github.lukmnh.toolbox.devutils.hash.domain.DigestEncoding;
import io.github.lukmnh.toolbox.devutils.hash.domain.DigestResult;
import io.github.lukmnh.toolbox.devutils.hash.domain.HmacAlgorithm;
import io.github.lukmnh.toolbox.devutils.hash.infrastructure.JdkTextHasher;
import java.util.List;
import org.junit.jupiter.api.Test;

class HmacTextUseCaseTest {

    /** RFC 2202 & RFC 4231 test case 2: key "Jefe". */
    private static final String MESSAGE = "what do ya want for nothing?";

    private final HmacTextUseCase useCase = new HmacTextUseCase(new JdkTextHasher());

    @Test
    void matchesRfcVectors() {
        List<DigestResult> results = useCase.execute(MESSAGE, "Jefe".toCharArray(), CancellationToken.none());

        assertEquals("750c783e6ab0b503eaa86e310a5db738", hex(results, HmacAlgorithm.HMAC_MD5));
        assertEquals("effcdf6ae5eb2fa2d27416d5f184df9c259a7c79", hex(results, HmacAlgorithm.HMAC_SHA1));
        assertEquals(
                "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
                hex(results, HmacAlgorithm.HMAC_SHA256));
        assertEquals(
                "af45d2e376484031617f78d2b58a6b1b9c7ef464f5a01b47e42ec3736322445e" + "8e2240ca5e69e2c78b3239ecfab21649",
                hex(results, HmacAlgorithm.HMAC_SHA384));
        assertEquals(
                "164b7a7bfcf819e2e395fbe73b56e0a387bd64222e831fd610270cd7ea250554"
                        + "9758bf75c05a994a6d034f65f8f0e6fdcaeab1a34d4a6b4b636e070a38bce737",
                hex(results, HmacAlgorithm.HMAC_SHA512));
    }

    @Test
    void keyIsWipedAfterUse() {
        char[] key = "Jefe".toCharArray();

        useCase.execute(MESSAGE, key, CancellationToken.none());

        assertArrayEquals(new char[4], key);
    }

    @Test
    void keyIsWipedWhenCancelled() {
        char[] key = "Jefe".toCharArray();
        CancellationToken token = new CancellationToken();
        token.cancel();

        assertThrows(TaskCancelledException.class, () -> useCase.execute(MESSAGE, key, token));
        assertArrayEquals(new char[4], key);
    }

    @Test
    void emptyKeyIsRejected() {
        assertThrows(
                IllegalArgumentException.class, () -> useCase.execute(MESSAGE, new char[0], CancellationToken.none()));
    }

    @Test
    void loneSurrogateKeyIsRejectedAndWiped() {
        char[] key = {'a', '\uD800'};

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(MESSAGE, key, CancellationToken.none()));
        assertArrayEquals(new char[2], key);
    }

    private static String hex(List<DigestResult> results, HmacAlgorithm algorithm) {
        return results.get(algorithm.ordinal()).encoded(DigestEncoding.HEX_LOWER);
    }
}
