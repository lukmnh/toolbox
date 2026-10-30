package io.github.lukmnh.toolbox.devutils.jwt.domain;

import java.util.Objects;

/**
 * Token JWS yang sudah dipisah dan di-decode Base64URL, belum di-parse sebagai JSON.
 *
 * @param headerJson teks JSON header (UTF-8)
 * @param payloadJson teks JSON payload (UTF-8)
 * @param signature byte signature; kosong untuk {@code alg: none}
 * @param signatureText segmen signature Base64URL apa adanya
 * @param signingInput {@code base64url(header) + "." + base64url(payload)} dalam ASCII, input verifikasi
 */
public record RawJwt(
        String headerJson, String payloadJson, byte[] signature, String signatureText, byte[] signingInput) {

    public RawJwt {
        Objects.requireNonNull(headerJson, "headerJson");
        Objects.requireNonNull(payloadJson, "payloadJson");
        Objects.requireNonNull(signatureText, "signatureText");
        signature = signature.clone();
        signingInput = signingInput.clone();
    }

    @Override
    public byte[] signature() {
        return signature.clone();
    }

    @Override
    public byte[] signingInput() {
        return signingInput.clone();
    }
}
