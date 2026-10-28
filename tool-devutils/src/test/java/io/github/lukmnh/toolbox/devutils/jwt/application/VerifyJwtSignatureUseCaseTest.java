package io.github.lukmnh.toolbox.devutils.jwt.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lukmnh.toolbox.devutils.jwt.domain.Claim;
import io.github.lukmnh.toolbox.devutils.jwt.domain.DecodedJwt;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JwtDecoder;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JwtSignatureVerifier;
import io.github.lukmnh.toolbox.devutils.jwt.domain.KeyMismatchException;
import io.github.lukmnh.toolbox.devutils.jwt.domain.KeyType;
import io.github.lukmnh.toolbox.devutils.jwt.domain.PublicKeyParser;
import io.github.lukmnh.toolbox.devutils.jwt.domain.RawJwt;
import io.github.lukmnh.toolbox.devutils.jwt.domain.SignatureStatus;
import io.github.lukmnh.toolbox.devutils.jwt.domain.UnreadableKeyException;
import io.github.lukmnh.toolbox.devutils.jwt.domain.UnsecuredJwtException;
import io.github.lukmnh.toolbox.devutils.jwt.domain.UnsupportedAlgorithmException;
import io.github.lukmnh.toolbox.devutils.jwt.testsupport.TestTokens;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.List;
import org.junit.jupiter.api.Test;

class VerifyJwtSignatureUseCaseTest {

    @Test
    void rfc7515HmacExampleWithBase64SecretIsValid() {
        char[] secret = TestTokens.RFC_HMAC_KEY.toCharArray();

        assertEquals(SignatureStatus.VALID, useCase(unusedParser()).execute(rfc(), new KeyInput.Secret(secret, true)));
    }

    @Test
    void wrongSecretIsInvalidAndSecretIsAlwaysWiped() {
        char[] secret = "bukan-secret".toCharArray();

        assertEquals(
                SignatureStatus.INVALID, useCase(unusedParser()).execute(rfc(), new KeyInput.Secret(secret, false)));
        assertWiped(secret);
    }

    @Test
    void secretIsWipedEvenWhenRejected() {
        char[] secret = "%%% bukan base64".toCharArray();

        UnreadableKeyException error = assertThrows(
                UnreadableKeyException.class,
                () -> useCase(unusedParser()).execute(rfc(), new KeyInput.Secret(secret, true)));
        assertEquals(UnreadableKeyException.Reason.INVALID_BASE64_SECRET, error.reason());
        assertWiped(secret);
    }

    @Test
    void utf8SecretMatchesHmacOverRawBytes() throws GeneralSecurityException {
        String token = TestTokens.hmac(
                "HS384", "HmacSHA384", "rahasia-ñ".getBytes(StandardCharsets.UTF_8), TestTokens.PAYLOAD);

        assertEquals(
                SignatureStatus.VALID,
                useCase(unusedParser())
                        .execute(decoded(token, "HS384"), new KeyInput.Secret("rahasia-ñ".toCharArray(), false)));
    }

    /** Serangan algorithm confusion klasik: PEM public key server dipakai sebagai secret HS256. */
    @Test
    void publicKeyTextInSecretFieldIsRejected() throws GeneralSecurityException {
        KeyPair rsa = TestTokens.rsa();
        String pem = TestTokens.pem(rsa.getPublic());
        String forged = TestTokens.hmac("HS256", "HmacSHA256", pem.getBytes(StandardCharsets.US_ASCII), "{}");
        char[] secret = pem.toCharArray();

        KeyMismatchException error = assertThrows(
                KeyMismatchException.class,
                () -> useCase(unusedParser()).execute(decoded(forged, "HS256"), new KeyInput.Secret(secret, false)));
        assertEquals(KeyType.PUBLIC_KEY_AS_SECRET, error.keyType());
        assertWiped(secret);
    }

    @Test
    void publicKeyForHmacAlgorithmIsRejected() throws GeneralSecurityException {
        KeyPair rsa = TestTokens.rsa();
        PublicKeyParser parser = text -> rsa.getPublic();

        assertThrows(
                KeyMismatchException.class,
                () -> useCase(parser).execute(rfc(), new KeyInput.PublicKeyText("-----BEGIN PUBLIC KEY-----")));
    }

    @Test
    void unsecuredAndUnknownAlgorithmsAreRejected() {
        DecodedJwt none = decoded(TestTokens.unsigned(TestTokens.header("none"), "{}") + ".", "none");
        DecodedJwt unknown = decoded(TestTokens.RFC_TOKEN, "HS999");
        DecodedJwt missing = decoded(TestTokens.RFC_TOKEN, "");
        KeyInput key = new KeyInput.PublicKeyText("x");

        assertThrows(UnsecuredJwtException.class, () -> useCase(unusedParser()).execute(none, key));
        char[] secret = "rahasia".toCharArray();
        assertThrows(
                UnsecuredJwtException.class,
                () -> useCase(unusedParser()).execute(none, new KeyInput.Secret(secret, false)));
        assertWiped(secret);
        UnsupportedAlgorithmException error = assertThrows(
                UnsupportedAlgorithmException.class,
                () -> useCase(unusedParser()).execute(unknown, key));
        assertEquals("HS999", error.algorithmName());
        assertThrows(
                UnsupportedAlgorithmException.class,
                () -> useCase(unusedParser()).execute(missing, key));
    }

    private static VerifyJwtSignatureUseCase useCase(PublicKeyParser parser) {
        return new VerifyJwtSignatureUseCase(parser, new JwtSignatureVerifier());
    }

    private static PublicKeyParser unusedParser() {
        return text -> {
            throw new AssertionError("Parser tidak boleh dipanggil");
        };
    }

    private static DecodedJwt rfc() {
        return decoded(TestTokens.RFC_TOKEN, "HS256");
    }

    private static DecodedJwt decoded(String token, String algorithm) {
        RawJwt raw = JwtDecoder.decode(token);
        return new DecodedJwt(raw, raw.headerJson(), raw.payloadJson(), algorithm, List.<Claim>of());
    }

    private static void assertWiped(char[] secret) {
        for (char value : secret) {
            assertEquals('\0', value);
        }
    }
}
