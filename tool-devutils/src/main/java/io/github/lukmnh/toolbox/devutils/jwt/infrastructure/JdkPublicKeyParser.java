package io.github.lukmnh.toolbox.devutils.jwt.infrastructure;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import io.github.lukmnh.toolbox.devutils.jwt.domain.PublicKeyParser;
import io.github.lukmnh.toolbox.devutils.jwt.domain.UnreadableKeyException;
import io.github.lukmnh.toolbox.devutils.jwt.domain.UnreadableKeyException.Reason;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EdECPoint;
import java.security.spec.EdECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.security.spec.NamedParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Membaca public key dari PEM ({@code BEGIN PUBLIC KEY}), sertifikat X.509 PEM, atau JWK (RSA, EC, OKP) dengan JDK
 * crypto saja. Tidak ada fetch JWKS dari URL (offline, Security #9).
 */
public final class JdkPublicKeyParser implements PublicKeyParser {

    private static final String PUBLIC_KEY_LABEL = "PUBLIC KEY";
    private static final String CERTIFICATE_LABEL = "CERTIFICATE";
    private static final String PRIVATE_KEY_MARKER = "PRIVATE KEY-----";
    private static final List<String> SPKI_KEY_ALGORITHMS = List.of("RSA", "EC", "EdDSA");
    private static final Map<String, String> EC_CURVES =
            Map.of("P-256", "secp256r1", "P-384", "secp384r1", "P-521", "secp521r1");
    private static final Map<String, NamedParameterSpec> EDWARDS_CURVES =
            Map.of("Ed25519", NamedParameterSpec.ED25519, "Ed448", NamedParameterSpec.ED448);
    private static final Map<String, Integer> EDWARDS_KEY_BYTES = Map.of("Ed25519", 32, "Ed448", 57);
    private static final int SIGN_BIT = 0x80;
    private static final int LOW_SEVEN_BITS = 0x7f;

    @Override
    public PublicKey parse(String text) {
        String trimmed = text.strip();
        if (trimmed.isEmpty()) {
            throw new UnreadableKeyException(Reason.EMPTY);
        }
        // Private key tidak pernah dibutuhkan untuk verifikasi; beri tahu user supaya tidak menyebarkannya
        if (trimmed.contains(PRIVATE_KEY_MARKER)) {
            throw new UnreadableKeyException(Reason.PRIVATE_KEY_GIVEN);
        }
        if (trimmed.startsWith("{")) {
            return fromJwk(trimmed);
        }
        if (trimmed.contains(begin(CERTIFICATE_LABEL))) {
            return fromCertificate(pemBlock(trimmed, CERTIFICATE_LABEL));
        }
        if (trimmed.contains(begin(PUBLIC_KEY_LABEL))) {
            return fromSubjectPublicKeyInfo(pemBody(trimmed, PUBLIC_KEY_LABEL));
        }
        throw new UnreadableKeyException(Reason.UNRECOGNIZED_FORMAT);
    }

    private static String begin(String label) {
        return "-----BEGIN " + label + "-----";
    }

    private static String end(String label) {
        return "-----END " + label + "-----";
    }

    private static String pemBlock(String text, String label) {
        int start = text.indexOf(begin(label));
        int stop = text.indexOf(end(label), start);
        if (stop < 0) {
            throw new UnreadableKeyException(Reason.INVALID_PEM);
        }
        return text.substring(start, stop + end(label).length());
    }

    private static byte[] pemBody(String text, String label) {
        String block = pemBlock(text, label);
        String body = block.substring(
                begin(label).length(), block.length() - end(label).length());
        try {
            return Base64.getDecoder().decode(body.replaceAll("\\s+", ""));
        } catch (IllegalArgumentException e) {
            throw new UnreadableKeyException(Reason.INVALID_PEM, e);
        }
    }

    /** Masa berlaku sertifikat sengaja tidak dicek: yang dibutuhkan hanya public key-nya. */
    private static PublicKey fromCertificate(String pem) {
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return factory.generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)))
                    .getPublicKey();
        } catch (CertificateException e) {
            throw new UnreadableKeyException(Reason.INVALID_CERTIFICATE, e);
        }
    }

    /** SPKI tidak menyebut algoritma secara eksplisit di PEM, jadi coba tiap KeyFactory yang relevan. */
    private static PublicKey fromSubjectPublicKeyInfo(byte[] der) {
        for (String algorithm : SPKI_KEY_ALGORITHMS) {
            try {
                return KeyFactory.getInstance(algorithm).generatePublic(new X509EncodedKeySpec(der));
            } catch (InvalidKeySpecException e) {
                // Bukan key jenis ini; lanjut ke KeyFactory berikutnya
                continue;
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("KeyFactory JDK tidak tersedia: " + algorithm, e);
            }
        }
        throw new UnreadableKeyException(Reason.UNSUPPORTED_KEY_TYPE);
    }

    private static PublicKey fromJwk(String json) {
        Map<String, String> fields = jwkFields(json);
        String keyType = fields.getOrDefault("kty", "");
        return switch (keyType) {
            case "RSA" -> generate("RSA", rsaSpec(fields));
            case "EC" -> generate("EC", ecSpec(fields));
            case "OKP" -> generate("EdDSA", edwardsSpec(fields));
            case "oct" -> throw new UnreadableKeyException(Reason.SYMMETRIC_JWK);
            case "" -> throw new UnreadableKeyException(Reason.INVALID_JWK);
            default -> throw new UnreadableKeyException(Reason.UNSUPPORTED_KEY_TYPE);
        };
    }

    /** Field string top-level JWK; field lain (misalnya {@code key_ops} array) dilewati. */
    private static Map<String, String> jwkFields(String json) {
        try (JsonParser parser = JwtJsonFactory.FACTORY.createParser(json)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw new UnreadableKeyException(Reason.INVALID_JWK);
            }
            Map<String, String> fields = new HashMap<>();
            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                String name = parser.currentName();
                if (parser.nextToken() == JsonToken.VALUE_STRING) {
                    fields.put(name, parser.getText());
                } else {
                    parser.skipChildren();
                }
            }
            return fields;
        } catch (JsonProcessingException e) {
            throw new UnreadableKeyException(Reason.INVALID_JWK, e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static KeySpec rsaSpec(Map<String, String> fields) {
        return new RSAPublicKeySpec(unsigned(fields, "n"), unsigned(fields, "e"));
    }

    private static KeySpec ecSpec(Map<String, String> fields) {
        String curve = EC_CURVES.get(fields.getOrDefault("crv", ""));
        if (curve == null) {
            throw new UnreadableKeyException(Reason.UNSUPPORTED_KEY_TYPE);
        }
        ECPoint point = new ECPoint(unsigned(fields, "x"), unsigned(fields, "y"));
        return new ECPublicKeySpec(point, curveParameters(curve));
    }

    private static ECParameterSpec curveParameters(String curve) {
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec(curve));
            return parameters.getParameterSpec(ECParameterSpec.class);
        } catch (InvalidParameterSpecException e) {
            throw new UnreadableKeyException(Reason.UNSUPPORTED_KEY_TYPE, e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Parameter kurva EC JDK tidak tersedia: " + curve, e);
        }
    }

    /**
     * RFC 8037: {@code x} adalah encoding little-endian koordinat y dengan bit tertinggi byte terakhir sebagai
     * paritas x.
     */
    private static KeySpec edwardsSpec(Map<String, String> fields) {
        String curve = fields.getOrDefault("crv", "");
        NamedParameterSpec parameters = EDWARDS_CURVES.get(curve);
        if (parameters == null) {
            throw new UnreadableKeyException(Reason.UNSUPPORTED_KEY_TYPE);
        }
        byte[] encoded = base64Url(fields, "x");
        if (encoded.length != EDWARDS_KEY_BYTES.get(curve)) {
            throw new UnreadableKeyException(Reason.INVALID_JWK);
        }
        int last = encoded.length - 1;
        boolean xOdd = (encoded[last] & SIGN_BIT) != 0;
        encoded[last] &= LOW_SEVEN_BITS;
        byte[] bigEndian = new byte[encoded.length];
        for (int index = 0; index < encoded.length; index++) {
            bigEndian[index] = encoded[last - index];
        }
        return new EdECPublicKeySpec(parameters, new EdECPoint(xOdd, new BigInteger(1, bigEndian)));
    }

    private static PublicKey generate(String algorithm, KeySpec spec) {
        try {
            return KeyFactory.getInstance(algorithm).generatePublic(spec);
        } catch (InvalidKeySpecException e) {
            throw new UnreadableKeyException(Reason.INVALID_JWK, e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("KeyFactory JDK tidak tersedia: " + algorithm, e);
        }
    }

    private static BigInteger unsigned(Map<String, String> fields, String name) {
        return new BigInteger(1, base64Url(fields, name));
    }

    private static byte[] base64Url(Map<String, String> fields, String name) {
        String value = fields.get(name);
        if (value == null || value.isEmpty()) {
            throw new UnreadableKeyException(Reason.INVALID_JWK);
        }
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new UnreadableKeyException(Reason.INVALID_JWK, e);
        }
    }
}
