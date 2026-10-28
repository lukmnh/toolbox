package io.github.lukmnh.toolbox.devutils.jwt.testsupport;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.EdECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Pembuat token, key pair, PEM, dan JWK on-the-fly untuk test JWT. */
public final class TestTokens {

    /** Contoh RFC 7519 §3.1 = RFC 7515 Lampiran A.1 (HS256). */
    public static final String RFC_TOKEN = "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9"
            + ".eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ"
            + ".dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

    /** Nilai {@code k} JWK dari RFC 7515 Lampiran A.1 (Base64URL). */
    public static final String RFC_HMAC_KEY =
            "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow";

    public static final String PAYLOAD = "{\"sub\":\"1234567890\",\"name\":\"Budi\",\"iat\":1516239022}";

    private TestTokens() {}

    public static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String base64Url(String text) {
        return base64Url(text.getBytes(StandardCharsets.UTF_8));
    }

    public static String header(String algorithm) {
        return "{\"alg\":\"" + algorithm + "\",\"typ\":\"JWT\"}";
    }

    public static String unsigned(String headerJson, String payloadJson) {
        return base64Url(headerJson) + "." + base64Url(payloadJson);
    }

    public static String hmac(String algorithm, String jcaName, byte[] key, String payloadJson)
            throws GeneralSecurityException {
        String input = unsigned(header(algorithm), payloadJson);
        Mac mac = Mac.getInstance(jcaName);
        mac.init(new SecretKeySpec(key, jcaName));
        return input + "." + base64Url(mac.doFinal(input.getBytes(StandardCharsets.US_ASCII)));
    }

    /** @param jcaName nama {@link Signature}; untuk PS* pakai {@code RSASSA-PSS} dan isi {@code pssHash} */
    public static String signed(String algorithm, String jcaName, String pssHash, PrivateKey key, String payloadJson)
            throws GeneralSecurityException {
        String input = unsigned(header(algorithm), payloadJson);
        Signature signature = Signature.getInstance(jcaName);
        signature.initSign(key);
        if (pssHash != null) {
            int saltBytes = Integer.parseInt(pssHash.substring(pssHash.indexOf('-') + 1)) / 8;
            signature.setParameter(new PSSParameterSpec(
                    pssHash, "MGF1", new MGF1ParameterSpec(pssHash), saltBytes, PSSParameterSpec.TRAILER_FIELD_BC));
        }
        signature.update(input.getBytes(StandardCharsets.US_ASCII));
        return input + "." + base64Url(signature.sign());
    }

    public static KeyPair rsa() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    public static KeyPair ec(String curve) throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec(curve));
        return generator.generateKeyPair();
    }

    public static KeyPair ed25519() throws GeneralSecurityException {
        return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
    }

    public static String pem(PublicKey key) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(key.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + body + "\n-----END PUBLIC KEY-----\n";
    }

    public static String jwk(RSAPublicKey key) {
        return "{\"kty\":\"RSA\",\"n\":\"" + base64Url(unsigned(key.getModulus(), 0)) + "\",\"e\":\""
                + base64Url(unsigned(key.getPublicExponent(), 0)) + "\",\"key_ops\":[\"verify\"]}";
    }

    public static String jwk(ECPublicKey key, String curve, int coordinateBytes) {
        return "{\"kty\":\"EC\",\"crv\":\"" + curve + "\",\"x\":\""
                + base64Url(unsigned(key.getW().getAffineX(), coordinateBytes)) + "\",\"y\":\""
                + base64Url(unsigned(key.getW().getAffineY(), coordinateBytes)) + "\"}";
    }

    /** RFC 8037: y little-endian 32 byte, bit tertinggi byte terakhir = paritas x. */
    public static String jwk(EdECPublicKey key) {
        byte[] bigEndian = unsigned(key.getPoint().getY(), 32);
        byte[] littleEndian = new byte[32];
        for (int index = 0; index < 32; index++) {
            littleEndian[index] = bigEndian[31 - index];
        }
        if (key.getPoint().isXOdd()) {
            littleEndian[31] |= (byte) 0x80;
        }
        return "{\"kty\":\"OKP\",\"crv\":\"Ed25519\",\"x\":\"" + base64Url(littleEndian) + "\"}";
    }

    /** @param length panjang tetap (padding nol di depan); 0 berarti panjang minimal */
    private static byte[] unsigned(BigInteger value, int length) {
        byte[] bytes = value.toByteArray();
        int start = bytes.length > 1 && bytes[0] == 0 ? 1 : 0;
        int size = bytes.length - start;
        int target = length == 0 ? size : length;
        byte[] result = new byte[target];
        System.arraycopy(bytes, start, result, target - size, size);
        return result;
    }
}
