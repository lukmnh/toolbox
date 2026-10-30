package io.github.lukmnh.toolbox.devutils.hash.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.TaskCancelledException;
import io.github.lukmnh.toolbox.devutils.hash.domain.KeyPairAlgorithm;
import io.github.lukmnh.toolbox.devutils.hash.domain.PemKeyPair;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class GenerateKeyPairUseCaseTest {

    private static final byte[] MESSAGE = "toolbox".getBytes(StandardCharsets.UTF_8);

    private final GenerateKeyPairUseCase useCase = new GenerateKeyPairUseCase(new SecureRandom());

    /** RSA 3072/4096 lambat dan jalurnya sama dengan 2048; cukup diuji lewat ukuran modulus di test terpisah. */
    @ParameterizedTest
    @EnumSource(
            value = KeyPairAlgorithm.class,
            names = {"RSA_2048", "EC_P256", "EC_P384", "EC_P521", "ED25519"})
    void pemRoundTripsThroughKeyFactoryAndKeysMatch(KeyPairAlgorithm algorithm) throws Exception {
        PemKeyPair pair = useCase.execute(algorithm, CancellationToken.none());

        KeyFactory factory = KeyFactory.getInstance(algorithm.family().jcaName());
        PublicKey publicKey = factory.generatePublic(new X509EncodedKeySpec(der(pair.publicKeyPem(), "PUBLIC KEY")));
        PrivateKey privateKey =
                factory.generatePrivate(new PKCS8EncodedKeySpec(der(pair.privateKeyPem(), "PRIVATE KEY")));

        // Private key harus pasangan public key: tanda tangan dari private terverifikasi oleh public
        Signature signer = Signature.getInstance(signatureAlgorithm(algorithm));
        signer.initSign(privateKey);
        signer.update(MESSAGE);
        byte[] signature = signer.sign();
        Signature verifier = Signature.getInstance(signatureAlgorithm(algorithm));
        verifier.initVerify(publicKey);
        verifier.update(MESSAGE);
        assertTrue(verifier.verify(signature));
        assertArrayEquals(der(pair.publicKeyPem(), "PUBLIC KEY"), publicKey.getEncoded());
    }

    @Test
    void rsaModulusAndEcCurveMatchSelection() throws Exception {
        PemKeyPair rsa = useCase.execute(KeyPairAlgorithm.RSA_2048, CancellationToken.none());
        RSAPublicKey rsaKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(der(rsa.publicKeyPem(), "PUBLIC KEY")));
        assertEquals(2048, rsaKey.getModulus().bitLength());

        PemKeyPair ec = useCase.execute(KeyPairAlgorithm.EC_P384, CancellationToken.none());
        ECPublicKey ecKey = (ECPublicKey) KeyFactory.getInstance("EC")
                .generatePublic(new X509EncodedKeySpec(der(ec.publicKeyPem(), "PUBLIC KEY")));
        assertEquals(384, ecKey.getParams().getCurve().getField().getFieldSize());
    }

    @Test
    void pemLinesAreWrappedAt64Columns() {
        PemKeyPair pair = useCase.execute(KeyPairAlgorithm.EC_P256, CancellationToken.none());

        for (String pem : List.of(pair.publicKeyPem(), pair.privateKeyPem())) {
            List<String> lines = pem.lines().toList();
            List<String> body = lines.subList(1, lines.size() - 1);
            assertTrue(body.stream().allMatch(line -> line.length() <= 64));
            assertTrue(body.subList(0, body.size() - 1).stream().allMatch(line -> line.length() == 64));
            assertTrue(pem.endsWith("-----\n"));
        }
        assertTrue(pair.publicKeyPem().startsWith("-----BEGIN PUBLIC KEY-----\n"));
        assertTrue(pair.privateKeyPem().startsWith("-----BEGIN PRIVATE KEY-----\n"));
    }

    @Test
    void toStringNeverContainsKeyMaterial() {
        PemKeyPair pair = useCase.execute(KeyPairAlgorithm.ED25519, CancellationToken.none());

        assertFalse(pair.toString().contains("PRIVATE"));
        assertEquals("PemKeyPair[Ed25519]", pair.toString());
    }

    @Test
    void cancelledRunDiscardsResult() {
        CancellationToken token = new CancellationToken();
        token.cancel();

        assertThrows(TaskCancelledException.class, () -> useCase.execute(KeyPairAlgorithm.EC_P256, token));
    }

    private static byte[] der(String pem, String label) {
        String body = pem.replace("-----BEGIN " + label + "-----", "")
                .replace("-----END " + label + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }

    private static String signatureAlgorithm(KeyPairAlgorithm algorithm) {
        return switch (algorithm.family()) {
            case RSA -> "SHA256withRSA";
            case EC -> "SHA256withECDSA";
            case ED25519 -> "Ed25519";
        };
    }
}
