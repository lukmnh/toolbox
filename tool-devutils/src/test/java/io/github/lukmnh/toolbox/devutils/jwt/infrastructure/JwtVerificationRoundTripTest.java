package io.github.lukmnh.toolbox.devutils.jwt.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lukmnh.toolbox.devutils.jwt.application.DecodeJwtUseCase;
import io.github.lukmnh.toolbox.devutils.jwt.application.KeyInput;
import io.github.lukmnh.toolbox.devutils.jwt.application.VerifyJwtSignatureUseCase;
import io.github.lukmnh.toolbox.devutils.jwt.domain.DecodedJwt;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JwtSignatureVerifier;
import io.github.lukmnh.toolbox.devutils.jwt.domain.KeyMismatchException;
import io.github.lukmnh.toolbox.devutils.jwt.domain.SignatureStatus;
import io.github.lukmnh.toolbox.devutils.jwt.testsupport.TestTokens;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.EdECPublicKey;
import java.security.interfaces.RSAPublicKey;
import org.junit.jupiter.api.Test;

/** Token ditandatangani di test dengan key pair baru, lalu diverifikasi lewat PEM dan JWK (stack nyata). */
class JwtVerificationRoundTripTest {

    private final DecodeJwtUseCase decode = new DecodeJwtUseCase(new JacksonJwtJson());
    private final VerifyJwtSignatureUseCase verify =
            new VerifyJwtSignatureUseCase(new JdkPublicKeyParser(), new JwtSignatureVerifier());

    @Test
    void rs256() throws GeneralSecurityException {
        KeyPair rsa = TestTokens.rsa();
        String token = TestTokens.signed("RS256", "SHA256withRSA", null, rsa.getPrivate(), TestTokens.PAYLOAD);

        assertValid(token, TestTokens.pem(rsa.getPublic()));
        assertValid(token, TestTokens.jwk((RSAPublicKey) rsa.getPublic()));
    }

    @Test
    void ps256AndPs512() throws GeneralSecurityException {
        KeyPair rsa = TestTokens.rsa();

        assertValid(
                TestTokens.signed("PS256", "RSASSA-PSS", "SHA-256", rsa.getPrivate(), TestTokens.PAYLOAD),
                TestTokens.pem(rsa.getPublic()));
        assertValid(
                TestTokens.signed("PS512", "RSASSA-PSS", "SHA-512", rsa.getPrivate(), TestTokens.PAYLOAD),
                TestTokens.jwk((RSAPublicKey) rsa.getPublic()));
    }

    @Test
    void es256() throws GeneralSecurityException {
        KeyPair ec = TestTokens.ec("secp256r1");
        String token =
                TestTokens.signed("ES256", "SHA256withECDSAinP1363Format", null, ec.getPrivate(), TestTokens.PAYLOAD);

        assertValid(token, TestTokens.pem(ec.getPublic()));
        assertValid(token, TestTokens.jwk((ECPublicKey) ec.getPublic(), "P-256", 32));
    }

    @Test
    void edDsa() throws GeneralSecurityException {
        KeyPair ed = TestTokens.ed25519();
        String token = TestTokens.signed("EdDSA", "Ed25519", null, ed.getPrivate(), TestTokens.PAYLOAD);

        assertValid(token, TestTokens.pem(ed.getPublic()));
        assertValid(token, TestTokens.jwk((EdECPublicKey) ed.getPublic()));
    }

    @Test
    void otherKeyIsInvalid() throws GeneralSecurityException {
        String token = TestTokens.signed(
                "RS256", "SHA256withRSA", null, TestTokens.rsa().getPrivate(), TestTokens.PAYLOAD);

        assertEquals(
                SignatureStatus.INVALID,
                verify.execute(
                        decode.execute(token),
                        new KeyInput.PublicKeyText(
                                TestTokens.pem(TestTokens.rsa().getPublic()))));
    }

    /** Header HS256 + PEM public key RSA ditolak, tidak pernah dicoba sebagai secret HMAC (Security #20). */
    @Test
    void algorithmConfusionIsRejected() throws GeneralSecurityException {
        KeyPair rsa = TestTokens.rsa();
        String pem = TestTokens.pem(rsa.getPublic());
        String forged =
                TestTokens.hmac("HS256", "HmacSHA256", pem.getBytes(java.nio.charset.StandardCharsets.US_ASCII), "{}");
        DecodedJwt jwt = decode.execute(forged);

        assertThrows(KeyMismatchException.class, () -> verify.execute(jwt, new KeyInput.PublicKeyText(pem)));
    }

    private void assertValid(String token, String key) {
        assertEquals(SignatureStatus.VALID, verify.execute(decode.execute(token), new KeyInput.PublicKeyText(key)));
    }
}
