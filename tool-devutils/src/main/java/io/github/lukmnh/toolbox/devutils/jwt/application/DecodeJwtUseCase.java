package io.github.lukmnh.toolbox.devutils.jwt.application;

import io.github.lukmnh.toolbox.devutils.jwt.domain.DecodedJwt;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JwtDecoder;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JwtJson;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JwtPart;
import io.github.lukmnh.toolbox.devutils.jwt.domain.RawJwt;

/** Decode token: pisah bagian, format JSON header & payload, ambil claims dan {@code alg}. */
public final class DecodeJwtUseCase {

    private final JwtJson json;

    public DecodeJwtUseCase(JwtJson json) {
        this.json = json;
    }

    /**
     * @throws io.github.lukmnh.toolbox.devutils.jwt.domain.MalformedJwtException bila token atau JSON rusak
     * @throws io.github.lukmnh.toolbox.devutils.jwt.domain.JweNotSupportedException untuk token JWE
     */
    public DecodedJwt execute(String token) {
        RawJwt raw = JwtDecoder.decode(token);
        String header = json.prettyPrint(raw.headerJson(), JwtPart.HEADER);
        String payload = json.prettyPrint(raw.payloadJson(), JwtPart.PAYLOAD);
        String algorithm = json.algorithm(raw.headerJson()).orElse("");
        return new DecodedJwt(raw, header, payload, algorithm, json.claims(raw.payloadJson()));
    }
}
