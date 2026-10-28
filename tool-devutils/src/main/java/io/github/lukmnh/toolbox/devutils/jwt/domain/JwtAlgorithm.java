package io.github.lukmnh.toolbox.devutils.jwt.domain;

import java.util.Arrays;
import java.util.Optional;

/** Algoritma JWS (RFC 7518 + RFC 8037) yang bisa diverifikasi dengan JDK crypto saja. */
public enum JwtAlgorithm {
    HS256("HS256", Family.HMAC, "HmacSHA256", 256),
    HS384("HS384", Family.HMAC, "HmacSHA384", 384),
    HS512("HS512", Family.HMAC, "HmacSHA512", 512),
    RS256("RS256", Family.RSA, "SHA256withRSA", 256),
    RS384("RS384", Family.RSA, "SHA384withRSA", 384),
    RS512("RS512", Family.RSA, "SHA512withRSA", 512),
    PS256("PS256", Family.RSA_PSS, "SHA-256", 256),
    PS384("PS384", Family.RSA_PSS, "SHA-384", 384),
    PS512("PS512", Family.RSA_PSS, "SHA-512", 512),
    ES256("ES256", Family.EC, "SHA256withECDSAinP1363Format", 256),
    ES384("ES384", Family.EC, "SHA384withECDSAinP1363Format", 384),
    ES512("ES512", Family.EC, "SHA512withECDSAinP1363Format", 512),
    EDDSA("EdDSA", Family.EDDSA, "EdDSA", 0),
    NONE("none", Family.NONE, "", 0);

    /** Kelompok algoritma; menentukan jenis key yang sah. */
    public enum Family {
        HMAC,
        RSA,
        RSA_PSS,
        EC,
        EDDSA,
        NONE
    }

    /** ES512 memakai kurva P-521, bukan P-512. */
    private static final int P521_BITS = 521;

    private static final int ES512_HASH_BITS = 512;

    private final String headerName;
    private final Family family;
    private final String jcaName;
    private final int hashBits;

    JwtAlgorithm(String headerName, Family family, String jcaName, int hashBits) {
        this.headerName = headerName;
        this.family = family;
        this.jcaName = jcaName;
        this.hashBits = hashBits;
    }

    /** Nilai {@code alg} di header case-sensitive sesuai RFC 7515, jadi {@code "hs256"} tidak dikenali. */
    public static Optional<JwtAlgorithm> fromHeader(String name) {
        return Arrays.stream(values())
                .filter(algorithm -> algorithm.headerName.equals(name))
                .findFirst();
    }

    public String headerName() {
        return headerName;
    }

    public Family family() {
        return family;
    }

    /** Nama JCA untuk {@code Mac}/{@code Signature}; untuk PS* berupa nama hash (dipakai di parameter PSS). */
    public String jcaName() {
        return jcaName;
    }

    public int hashBits() {
        return hashBits;
    }

    /** Ukuran field kurva EC yang wajib dipakai; 0 untuk algoritma non-EC. */
    public int ecCurveBits() {
        if (family != Family.EC) {
            return 0;
        }
        return hashBits == ES512_HASH_BITS ? P521_BITS : hashBits;
    }
}
