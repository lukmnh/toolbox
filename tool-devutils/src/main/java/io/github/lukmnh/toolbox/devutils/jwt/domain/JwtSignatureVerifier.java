package io.github.lukmnh.toolbox.devutils.jwt.domain;

import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.EdECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Verifikasi signature JWS dengan JDK crypto saja. Aturan keamanan (Security #20):
 *
 * <ul>
 *   <li>{@code alg: none} tidak pernah valid.
 *   <li>Jenis key wajib cocok dengan {@code alg}; public key tidak pernah dipakai sebagai secret HMAC.
 *   <li>HMAC dibandingkan constant-time ({@link MessageDigest#isEqual}).
 * </ul>
 */
public final class JwtSignatureVerifier {

    private static final String PSS_ALGORITHM = "RSASSA-PSS";
    private static final String MGF1 = "MGF1";
    private static final int BITS_PER_BYTE = 8;

    /**
     * @return {@code true} bila signature cocok
     * @throws UnsecuredJwtException untuk {@code alg: none}
     * @throws KeyMismatchException bila jenis key tidak cocok dengan algoritma
     * @throws UnreadableKeyException bila key tidak bisa dipakai oleh provider JDK (misalnya RSA terlalu pendek)
     */
    public boolean verify(RawJwt jwt, JwtAlgorithm algorithm, VerificationKey key) {
        return switch (algorithm.family()) {
            case NONE -> throw new UnsecuredJwtException();
            case HMAC -> verifyHmac(jwt, algorithm, requireSecret(algorithm, key));
            case RSA, RSA_PSS, EC, EDDSA -> verifySignature(jwt, algorithm, requirePublicKey(algorithm, key));
        };
    }

    private static VerificationKey.HmacSecret requireSecret(JwtAlgorithm algorithm, VerificationKey key) {
        return switch (key) {
            case VerificationKey.HmacSecret secret -> secret;
            case VerificationKey.Public publicKey -> throw mismatch(algorithm, publicKey.key());
        };
    }

    private static PublicKey requirePublicKey(JwtAlgorithm algorithm, VerificationKey key) {
        PublicKey publicKey = switch (key) {
            case VerificationKey.HmacSecret secret -> throw new KeyMismatchException(algorithm, KeyType.HMAC_SECRET);
            case VerificationKey.Public material -> material.key();
        };
        if (!matchesFamily(algorithm, publicKey)) {
            throw mismatch(algorithm, publicKey);
        }
        return publicKey;
    }

    private static boolean matchesFamily(JwtAlgorithm algorithm, PublicKey key) {
        return switch (algorithm.family()) {
            case RSA, RSA_PSS -> key instanceof RSAPublicKey;
            // ES384 dengan key P-256 ditolak: kurva menentukan kekuatan, bukan hanya hash
            case EC -> key instanceof ECPublicKey ec && curveBits(ec) == algorithm.ecCurveBits();
            case EDDSA -> key instanceof EdECPublicKey;
            case HMAC, NONE -> false;
        };
    }

    private static KeyMismatchException mismatch(JwtAlgorithm algorithm, PublicKey key) {
        int bits = key instanceof ECPublicKey ec ? curveBits(ec) : 0;
        return new KeyMismatchException(algorithm, KeyType.of(key), bits);
    }

    private static int curveBits(ECPublicKey key) {
        return key.getParams().getCurve().getField().getFieldSize();
    }

    private static boolean verifyHmac(RawJwt jwt, JwtAlgorithm algorithm, VerificationKey.HmacSecret secret) {
        if (secret.bytes().length == 0) {
            throw new UnreadableKeyException(UnreadableKeyException.Reason.EMPTY);
        }
        try {
            Mac mac = Mac.getInstance(algorithm.jcaName());
            // SecretKeySpec menyimpan salinan internal yang tidak bisa di-wipe; masa hidupnya hanya method ini
            mac.init(new SecretKeySpec(secret.bytes(), algorithm.jcaName()));
            byte[] expected = mac.doFinal(jwt.signingInput());
            return MessageDigest.isEqual(expected, jwt.signature());
        } catch (InvalidKeyException e) {
            throw new UnreadableKeyException(UnreadableKeyException.Reason.UNUSABLE_KEY, e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("JDK HMAC algorithm not available: " + algorithm.jcaName(), e);
        }
    }

    private static boolean verifySignature(RawJwt jwt, JwtAlgorithm algorithm, PublicKey key) {
        try {
            Signature signature = signatureFor(algorithm);
            signature.initVerify(key);
            if (algorithm.family() == JwtAlgorithm.Family.RSA_PSS) {
                signature.setParameter(pssParameters(algorithm));
            }
            signature.update(jwt.signingInput());
            return signature.verify(jwt.signature());
        } catch (SignatureException e) {
            // Panjang/format signature salah berarti signature tidak cocok, bukan error aplikasi
            return false;
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new UnreadableKeyException(UnreadableKeyException.Reason.UNUSABLE_KEY, e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("JDK signature algorithm not available: " + algorithm.jcaName(), e);
        }
    }

    private static Signature signatureFor(JwtAlgorithm algorithm) throws GeneralSecurityException {
        String name = algorithm.family() == JwtAlgorithm.Family.RSA_PSS ? PSS_ALGORITHM : algorithm.jcaName();
        return Signature.getInstance(name);
    }

    /** RFC 7518 §3.5: MGF1 dengan hash yang sama, salt sepanjang output hash. */
    private static PSSParameterSpec pssParameters(JwtAlgorithm algorithm) {
        String hash = algorithm.jcaName();
        return new PSSParameterSpec(
                hash,
                MGF1,
                new MGF1ParameterSpec(hash),
                algorithm.hashBits() / BITS_PER_BYTE,
                PSSParameterSpec.TRAILER_FIELD_BC);
    }
}
