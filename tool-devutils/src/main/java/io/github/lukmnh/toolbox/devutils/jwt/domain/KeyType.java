package io.github.lukmnh.toolbox.devutils.jwt.domain;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.EdECPublicKey;
import java.security.interfaces.RSAPublicKey;

/** Jenis key yang diberikan user, untuk pesan algorithm confusion yang jelas. */
public enum KeyType {
    HMAC_SECRET,
    /** Teks public key (PEM/JWK) yang ditempel di kolom secret HMAC: pola serangan algorithm confusion klasik. */
    PUBLIC_KEY_AS_SECRET,
    RSA,
    EC,
    EDDSA,
    UNKNOWN;

    public static KeyType of(PublicKey key) {
        return switch (key) {
            case RSAPublicKey rsa -> RSA;
            case ECPublicKey ec -> EC;
            case EdECPublicKey edEc -> EDDSA;
            default -> UNKNOWN;
        };
    }
}
