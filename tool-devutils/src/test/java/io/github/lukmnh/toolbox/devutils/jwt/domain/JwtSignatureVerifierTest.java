package io.github.lukmnh.toolbox.devutils.jwt.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.devutils.jwt.testsupport.TestTokens;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtSignatureVerifierTest {

    private final JwtSignatureVerifier verifier = new JwtSignatureVerifier();

    @Test
    void rfc7515HmacExampleIsValid() {
        RawJwt jwt = JwtDecoder.decode(TestTokens.RFC_TOKEN);
        byte[] key = Base64.getUrlDecoder().decode(TestTokens.RFC_HMAC_KEY);

        assertTrue(verifier.verify(jwt, JwtAlgorithm.HS256, new VerificationKey.HmacSecret(key)));
        assertFalse(verifier.verify(jwt, JwtAlgorithm.HS256, secret("salah")));
    }

    @Test
    void unsecuredTokenIsNeverValid() {
        RawJwt jwt = JwtDecoder.decode(TestTokens.unsigned(TestTokens.header("none"), "{}") + ".");

        assertThrows(UnsecuredJwtException.class, () -> verifier.verify(jwt, JwtAlgorithm.NONE, secret("x")));
    }

    @Test
    void publicKeyIsNeverUsedAsHmacSecret() throws GeneralSecurityException {
        RawJwt jwt = JwtDecoder.decode(TestTokens.RFC_TOKEN);
        KeyPair rsa = TestTokens.rsa();

        KeyMismatchException error = assertThrows(
                KeyMismatchException.class,
                () -> verifier.verify(jwt, JwtAlgorithm.HS256, new VerificationKey.Public(rsa.getPublic())));
        assertEquals(KeyType.RSA, error.keyType());
    }

    @Test
    void secretIsNeverUsedForAsymmetricAlgorithm() {
        RawJwt jwt = JwtDecoder.decode(TestTokens.RFC_TOKEN);

        assertThrows(KeyMismatchException.class, () -> verifier.verify(jwt, JwtAlgorithm.RS256, secret("x")));
    }

    @Test
    void keyFamilyAndCurveMustMatchAlgorithm() throws GeneralSecurityException {
        KeyPair p256 = TestTokens.ec("secp256r1");
        RawJwt jwt = JwtDecoder.decode(TestTokens.signed(
                "ES256", "SHA256withECDSAinP1363Format", null, p256.getPrivate(), TestTokens.PAYLOAD));
        VerificationKey key = new VerificationKey.Public(p256.getPublic());

        assertTrue(verifier.verify(jwt, JwtAlgorithm.ES256, key));
        KeyMismatchException curve =
                assertThrows(KeyMismatchException.class, () -> verifier.verify(jwt, JwtAlgorithm.ES384, key));
        assertEquals(256, curve.curveBits());
        assertThrows(KeyMismatchException.class, () -> verifier.verify(jwt, JwtAlgorithm.RS256, key));
    }

    @Test
    void tamperedPayloadIsInvalid() throws GeneralSecurityException {
        KeyPair rsa = TestTokens.rsa();
        String token = TestTokens.signed("RS256", "SHA256withRSA", null, rsa.getPrivate(), TestTokens.PAYLOAD);
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + TestTokens.base64Url("{\"sub\":\"admin\"}") + "." + parts[2];

        assertFalse(verifier.verify(
                JwtDecoder.decode(tampered), JwtAlgorithm.RS256, new VerificationKey.Public(rsa.getPublic())));
    }

    @Test
    void malformedEcdsaSignatureIsInvalidNotAnError() throws GeneralSecurityException {
        KeyPair p256 = TestTokens.ec("secp256r1");
        RawJwt jwt = JwtDecoder.decode(TestTokens.unsigned(TestTokens.header("ES256"), "{}") + ".AAAA");

        assertFalse(verifier.verify(jwt, JwtAlgorithm.ES256, new VerificationKey.Public(p256.getPublic())));
    }

    @Test
    void emptySecretIsRejected() {
        RawJwt jwt = JwtDecoder.decode(TestTokens.RFC_TOKEN);

        UnreadableKeyException error = assertThrows(
                UnreadableKeyException.class,
                () -> verifier.verify(jwt, JwtAlgorithm.HS256, new VerificationKey.HmacSecret(new byte[0])));
        assertEquals(UnreadableKeyException.Reason.EMPTY, error.reason());
    }

    @Test
    void wipeClearsSecretBytes() {
        VerificationKey.HmacSecret secret = secret("rahasia");

        secret.wipe();

        for (byte value : secret.bytes()) {
            assertEquals(0, value);
        }
    }

    private static VerificationKey.HmacSecret secret(String text) {
        return new VerificationKey.HmacSecret(text.getBytes(StandardCharsets.UTF_8));
    }
}
