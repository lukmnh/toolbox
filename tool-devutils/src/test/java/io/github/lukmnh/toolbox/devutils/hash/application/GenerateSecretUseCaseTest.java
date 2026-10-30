package io.github.lukmnh.toolbox.devutils.hash.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.devutils.hash.domain.SecretEncoding;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Random;
import org.junit.jupiter.api.Test;

class GenerateSecretUseCaseTest {

    private final GenerateSecretUseCase useCase = new GenerateSecretUseCase(new SecureRandom());

    @Test
    void encodedLengthMatchesByteLength() {
        for (int bytes : GenerateSecretUseCase.BYTE_LENGTHS) {
            assertEquals(bytes, HexFormat.of().parseHex(useCase.execute(bytes, SecretEncoding.HEX)).length);
            assertEquals(bytes, Base64.getDecoder().decode(useCase.execute(bytes, SecretEncoding.BASE64)).length);
            assertEquals(
                    bytes, Base64.getUrlDecoder().decode(useCase.execute(bytes, SecretEncoding.BASE64_URL)).length);
        }
    }

    @Test
    void base64UrlIsUrlSafeWithoutPadding() {
        for (int attempt = 0; attempt < 50; attempt++) {
            String secret = useCase.execute(16, SecretEncoding.BASE64_URL);
            assertTrue(secret.matches("[A-Za-z0-9_-]{22}"), secret);
        }
    }

    @Test
    void encodesBytesFromRandomSource() {
        GenerateSecretUseCase seeded = new GenerateSecretUseCase(new Random(42));
        byte[] expected = new byte[16];
        new Random(42).nextBytes(expected);

        assertArrayEquals(expected, HexFormat.of().parseHex(seeded.execute(16, SecretEncoding.HEX)));
    }

    @Test
    void consecutiveSecretsDiffer() {
        assertNotEquals(useCase.execute(32, SecretEncoding.HEX), useCase.execute(32, SecretEncoding.HEX));
    }

    @Test
    void rejectsOutOfRangeLength() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(0, SecretEncoding.HEX));
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(GenerateSecretUseCase.MAX_BYTES + 1, SecretEncoding.HEX));
    }
}
