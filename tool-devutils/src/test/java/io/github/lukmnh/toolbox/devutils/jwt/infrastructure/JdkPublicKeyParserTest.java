package io.github.lukmnh.toolbox.devutils.jwt.infrastructure;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lukmnh.toolbox.devutils.jwt.domain.UnreadableKeyException;
import io.github.lukmnh.toolbox.devutils.jwt.domain.UnreadableKeyException.Reason;
import io.github.lukmnh.toolbox.devutils.jwt.testsupport.TestTokens;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.EdECPublicKey;
import java.security.interfaces.RSAPublicKey;
import org.junit.jupiter.api.Test;

class JdkPublicKeyParserTest {

    /** Sertifikat self-signed EC P-256 (dibuat dengan keytool, berlaku sampai 2126). */
    private static final String CERTIFICATE = """
            -----BEGIN CERTIFICATE-----
            MIIBRjCB7aADAgECAggWEq6kUN3ZcDAKBggqhkjOPQQDAzAXMRUwEwYDVQQDEwx0
            b29sYm94LXRlc3QwIBcNMjYxMDI4MDc1OTAzWhgPMjEyNjEwMDQwNzU5MDNaMBcx
            FTATBgNVBAMTDHRvb2xib3gtdGVzdDBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IA
            BN1ggZYbR1NTu/kvMPPEckaXGA6IgnTOvWbOVgn4o5AkKjkPSetwbnj8XWRRJp+p
            xKQigBQmNywlZMDOgkaWyHOjITAfMB0GA1UdDgQWBBSt2sx2JlhBELltMev++RkJ
            x+MWuTAKBggqhkjOPQQDAwNIADBFAiAav2GeXAcGRUoio//7HpwDaH0v+dlHu10A
            QjZ5f/KuPQIhAOYOSxjKFHV9wpaa4cIYjnqpblUHj7QfiRCuauePUPfP
            -----END CERTIFICATE-----
            """;

    private final JdkPublicKeyParser parser = new JdkPublicKeyParser();

    @Test
    void readsPemForEveryKeyFamily() throws GeneralSecurityException {
        for (KeyPair pair : new KeyPair[] {TestTokens.rsa(), TestTokens.ec("secp384r1"), TestTokens.ed25519()}) {
            PublicKey parsed = parser.parse("  \n" + TestTokens.pem(pair.getPublic()) + "\n");
            assertArrayEquals(pair.getPublic().getEncoded(), parsed.getEncoded());
        }
    }

    @Test
    void readsJwkForEveryKeyFamily() throws GeneralSecurityException {
        KeyPair rsa = TestTokens.rsa();
        KeyPair ec = TestTokens.ec("secp521r1");
        KeyPair ed = TestTokens.ed25519();

        assertArrayEquals(
                rsa.getPublic().getEncoded(),
                parser.parse(TestTokens.jwk((RSAPublicKey) rsa.getPublic())).getEncoded());
        assertArrayEquals(
                ec.getPublic().getEncoded(),
                parser.parse(TestTokens.jwk((ECPublicKey) ec.getPublic(), "P-521", 66))
                        .getEncoded());
        assertArrayEquals(
                ed.getPublic().getEncoded(),
                parser.parse(TestTokens.jwk((EdECPublicKey) ed.getPublic())).getEncoded());
    }

    @Test
    void readsPublicKeyFromCertificate() {
        assertInstanceOf(ECPublicKey.class, parser.parse(CERTIFICATE));
    }

    @Test
    void privateKeyIsRefused() {
        assertReason("-----BEGIN PRIVATE KEY-----\nMIIE\n-----END PRIVATE KEY-----", Reason.PRIVATE_KEY_GIVEN);
        assertReason("-----BEGIN RSA PRIVATE KEY-----\nMIIE\n-----END RSA PRIVATE KEY-----", Reason.PRIVATE_KEY_GIVEN);
    }

    @Test
    void brokenInputsHaveSpecificReasons() {
        assertReason("   ", Reason.EMPTY);
        assertReason("ssh-rsa AAAAB3NzaC1yc2E", Reason.UNRECOGNIZED_FORMAT);
        assertReason("-----BEGIN PUBLIC KEY-----\nMIIB", Reason.INVALID_PEM);
        assertReason("-----BEGIN PUBLIC KEY-----\n%%%\n-----END PUBLIC KEY-----", Reason.INVALID_PEM);
        assertReason("-----BEGIN PUBLIC KEY-----\nAAAA\n-----END PUBLIC KEY-----", Reason.UNSUPPORTED_KEY_TYPE);
        assertReason("-----BEGIN CERTIFICATE-----\nAAAA\n-----END CERTIFICATE-----", Reason.INVALID_CERTIFICATE);
        assertReason("{\"kty\":\"oct\",\"k\":\"AyM1\"}", Reason.SYMMETRIC_JWK);
        assertReason("{\"kty\":\"RSA\",\"n\":\"AQAB\"}", Reason.INVALID_JWK);
        assertReason("{\"kty\":\"EC\",\"crv\":\"secp256k1\",\"x\":\"AA\",\"y\":\"AA\"}", Reason.UNSUPPORTED_KEY_TYPE);
        assertReason("{\"kty\":\"OKP\",\"crv\":\"Ed25519\",\"x\":\"AAAA\"}", Reason.INVALID_JWK);
        assertReason("{\"kty\":", Reason.INVALID_JWK);
        assertReason("{\"n\":\"AQAB\"}", Reason.INVALID_JWK);
    }

    private void assertReason(String text, Reason reason) {
        UnreadableKeyException error = assertThrows(UnreadableKeyException.class, () -> parser.parse(text));
        assertEquals(reason, error.reason());
    }
}
