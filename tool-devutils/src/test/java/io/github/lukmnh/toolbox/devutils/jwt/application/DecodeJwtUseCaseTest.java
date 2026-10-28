package io.github.lukmnh.toolbox.devutils.jwt.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lukmnh.toolbox.devutils.jwt.domain.Claim;
import io.github.lukmnh.toolbox.devutils.jwt.domain.DecodedJwt;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JweNotSupportedException;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JwtAlgorithm;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JwtJson;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JwtPart;
import io.github.lukmnh.toolbox.devutils.jwt.domain.MalformedJwtException;
import io.github.lukmnh.toolbox.devutils.jwt.testsupport.TestTokens;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DecodeJwtUseCaseTest {

    /** Fake port: "pretty print" menandai bagian, claims & alg tetap. */
    private static final JwtJson FAKE_JSON = new JwtJson() {
        @Override
        public String prettyPrint(String json, JwtPart part) {
            return part + ":" + json;
        }

        @Override
        public List<Claim> claims(String payloadJson) {
            return List.of(new Claim("iss", "joe", Claim.ValueType.STRING));
        }

        @Override
        public Optional<String> algorithm(String headerJson) {
            return headerJson.contains("HS256") ? Optional.of("HS256") : Optional.empty();
        }
    };

    private final DecodeJwtUseCase useCase = new DecodeJwtUseCase(FAKE_JSON);

    @Test
    void decodesHeaderPayloadClaimsAndAlgorithm() {
        DecodedJwt jwt = useCase.execute("Bearer " + TestTokens.RFC_TOKEN);

        assertEquals("HEADER:{\"typ\":\"JWT\",\r\n \"alg\":\"HS256\"}", jwt.prettyHeader());
        assertEquals("HS256", jwt.algorithmName());
        assertEquals(JwtAlgorithm.HS256, jwt.algorithm().orElseThrow());
        assertEquals("joe", jwt.claims().getFirst().value());
    }

    @Test
    void missingAlgorithmIsEmpty() {
        DecodedJwt jwt = useCase.execute(TestTokens.unsigned("{}", "{}") + ".");

        assertEquals("", jwt.algorithmName());
        assertEquals(Optional.empty(), jwt.algorithm());
    }

    @Test
    void structuralErrorsPropagate() {
        assertThrows(JweNotSupportedException.class, () -> useCase.execute("a.b.c.d.e"));
        assertThrows(MalformedJwtException.class, () -> useCase.execute("a.b"));
    }
}
