package io.github.lukmnh.toolbox.devutils.hash.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.devutils.hash.domain.DigestEncoding;
import io.github.lukmnh.toolbox.devutils.hash.domain.DigestResult;
import io.github.lukmnh.toolbox.devutils.hash.domain.HashAlgorithm;
import io.github.lukmnh.toolbox.devutils.hash.domain.HmacAlgorithm;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class JdkTextHasherTest {

    private final JdkTextHasher hasher = new JdkTextHasher();

    /** Test vector resmi (FIPS 180-4 / FIPS 202 / RFC 1321) untuk string kosong dan "abc". */
    @ParameterizedTest
    @CsvSource({
        "MD5, '', d41d8cd98f00b204e9800998ecf8427e",
        "MD5, abc, 900150983cd24fb0d6963f7d28e17f72",
        "SHA_1, '', da39a3ee5e6b4b0d3255bfef95601890afd80709",
        "SHA_1, abc, a9993e364706816aba3e25717850c26c9cd0d89d",
        "SHA_256, '', e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        "SHA_256, abc, ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
        "SHA_384, '', 38b060a751ac96384cd9327eb1b1e36a21fdb71114be07434c0cc7bf63f6e1da274edebfe76f65fbd51ad2f14898b95b",
        "SHA_384, abc, cb00753f45a35e8bb5a03d699ac65007272c32ab0eded1631a8b605a43ff5bed8086072ba1e7cc2358baeca134c825a7",
        "SHA_512, '', cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
        "SHA_512, abc, ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
        "SHA3_256, '', a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a",
        "SHA3_256, abc, 3a985da74fe225b2045c172d6bd390bd855f086e3e9d525b46bfe24511431532",
        "SHA3_512, '', a69f73cca23a9ac5c8b567dc185a756e97c982164fe25859e0d1dcc1475c80a615b2123af1f5f94c11e3e9402c3ac558f500199d95b6d3e301758586281dcd26",
        "SHA3_512, abc, b751850b1a57168a5693cd924b6b096e08f621827444f70d884f5d0240d2712e10e116e9192af3c91a7ec57647e3934057340b4cf408d5a56592f8274eec53f0",
    })
    void digestMatchesOfficialVectors(HashAlgorithm algorithm, String input, String expectedHex) {
        DigestResult result = hasher.digest(algorithm, input.getBytes(StandardCharsets.UTF_8));

        assertEquals(expectedHex, result.encoded(DigestEncoding.HEX_LOWER));
        assertEquals(algorithm.label(), result.algorithm());
    }

    @Test
    void onlyMd5AndSha1AreMarkedInsecure() {
        for (HashAlgorithm algorithm : HashAlgorithm.values()) {
            boolean expected = algorithm == HashAlgorithm.MD5 || algorithm == HashAlgorithm.SHA_1;
            assertEquals(expected, hasher.digest(algorithm, new byte[0]).insecure(), algorithm.name());
        }
    }

    @Test
    void encodingsFormatTheSameDigest() {
        DigestResult result = hasher.digest(HashAlgorithm.SHA_256, "abc".getBytes(StandardCharsets.UTF_8));

        assertEquals(
                "BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD",
                result.encoded(DigestEncoding.HEX_UPPER));
        assertEquals("ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=", result.encoded(DigestEncoding.BASE64));
    }

    @Test
    void digestIsDefensivelyCopied() {
        DigestResult result = hasher.digest(HashAlgorithm.MD5, new byte[0]);
        result.digest()[0] = 0;

        assertEquals("d41d8cd98f00b204e9800998ecf8427e", result.encoded(DigestEncoding.HEX_LOWER));
    }

    @Test
    void toStringDoesNotLeakDigest() {
        DigestResult result = hasher.digest(HashAlgorithm.MD5, new byte[0]);

        assertFalse(result.toString().contains("d41d8cd9"));
        assertTrue(result.toString().contains("MD5"));
    }

    @Test
    void emptyHmacKeyIsRejected() {
        assertThrows(
                IllegalArgumentException.class, () -> hasher.hmac(HmacAlgorithm.HMAC_SHA256, new byte[0], new byte[0]));
    }
}
