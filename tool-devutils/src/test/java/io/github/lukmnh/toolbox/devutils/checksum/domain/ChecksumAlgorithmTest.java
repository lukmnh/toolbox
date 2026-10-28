package io.github.lukmnh.toolbox.devutils.checksum.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ChecksumAlgorithmTest {

    @Test
    void detectsAlgorithmFromHexLength() {
        assertEquals(Optional.of(ChecksumAlgorithm.CRC32), ChecksumAlgorithm.detect("352441c2"));
        assertEquals(Optional.of(ChecksumAlgorithm.MD5), ChecksumAlgorithm.detect("a".repeat(32)));
        assertEquals(Optional.of(ChecksumAlgorithm.SHA_1), ChecksumAlgorithm.detect("b".repeat(40)));
        assertEquals(Optional.of(ChecksumAlgorithm.SHA_256), ChecksumAlgorithm.detect("c".repeat(64)));
        assertEquals(Optional.of(ChecksumAlgorithm.SHA_512), ChecksumAlgorithm.detect("d".repeat(128)));
    }

    @Test
    void detectionTrimsAndIgnoresCase() {
        assertEquals(Optional.of(ChecksumAlgorithm.CRC32), ChecksumAlgorithm.detect("  352441C2 \n"));
        assertEquals("352441c2", ChecksumAlgorithm.normalize("  352441C2 "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "352441c", "352441cz", "zzzzzzzz", "abcdef0123"})
    void unknownLengthOrNonHexIsNotDetected(String checksum) {
        assertTrue(ChecksumAlgorithm.detect(checksum).isEmpty());
    }

    @Test
    void sumsFileNameFollowsCoreutilsConvention() {
        assertEquals("SHA256SUMS", ChecksumAlgorithm.DEFAULT.sumsFileName());
        assertEquals("MD5SUMS", ChecksumAlgorithm.MD5.sumsFileName());
    }
}
