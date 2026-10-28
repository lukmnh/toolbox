package io.github.lukmnh.toolbox.devutils.jwt.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lukmnh.toolbox.devutils.jwt.domain.Claim;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JwtPart;
import io.github.lukmnh.toolbox.devutils.jwt.domain.MalformedJwtException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JacksonJwtJsonTest {

    private final JacksonJwtJson json = new JacksonJwtJson();

    @Test
    void prettyPrintsWithTwoSpaceIndent() {
        assertEquals(
                "{\n  \"alg\": \"HS256\",\n  \"crit\": [\n    \"b64\"\n  ]\n}",
                json.prettyPrint("{\"alg\":\"HS256\",\"crit\":[\"b64\"]}", JwtPart.HEADER));
    }

    @Test
    void emojiIsKeptAsIs() {
        assertEquals("{\n  \"name\": \"Budi 😀\"\n}", json.prettyPrint("{\"name\":\"Budi 😀\"}", JwtPart.PAYLOAD));
    }

    @Test
    void nonObjectAndInvalidJsonPointAtPart() {
        assertMalformed(
                () -> json.prettyPrint("[1,2]", JwtPart.PAYLOAD),
                JwtPart.PAYLOAD,
                MalformedJwtException.Reason.NOT_JSON_OBJECT);
        MalformedJwtException broken = assertMalformed(
                () -> json.prettyPrint("{\"a\":}", JwtPart.HEADER),
                JwtPart.HEADER,
                MalformedJwtException.Reason.INVALID_JSON);
        assertEquals("1:6", broken.detail());
        assertMalformed(
                () -> json.prettyPrint("{}x", JwtPart.PAYLOAD),
                JwtPart.PAYLOAD,
                MalformedJwtException.Reason.INVALID_JSON);
    }

    @Test
    void duplicateClaimsAreRejected() {
        assertMalformed(
                () -> json.prettyPrint("{\"exp\":1,\"exp\":2}", JwtPart.PAYLOAD),
                JwtPart.PAYLOAD,
                MalformedJwtException.Reason.INVALID_JSON);
    }

    @Test
    void deepNestingIsRejectedCleanly() {
        String deep = "{\"a\":" + "[".repeat(100_000) + "]".repeat(100_000) + "}";

        assertMalformed(
                () -> json.prettyPrint(deep, JwtPart.PAYLOAD),
                JwtPart.PAYLOAD,
                MalformedJwtException.Reason.INVALID_JSON);
    }

    @Test
    void claimsKeepOrderAndTypes() {
        List<Claim> claims = json.claims(
                "{\"iss\":\"joe\",\"exp\":1300819380,\"aud\":[\"a\",\"b\"],\"admin\":true,\"x\":null,\"o\":{\"k\":1}}");

        assertEquals(
                List.of(
                        new Claim("iss", "joe", Claim.ValueType.STRING),
                        new Claim("exp", "1300819380", Claim.ValueType.NUMBER),
                        new Claim("aud", "[\"a\",\"b\"]", Claim.ValueType.ARRAY),
                        new Claim("admin", "true", Claim.ValueType.BOOLEAN),
                        new Claim("x", "null", Claim.ValueType.NULL),
                        new Claim("o", "{\"k\":1}", Claim.ValueType.OBJECT)),
                claims);
    }

    @Test
    void algorithmIsReadOnlyFromStringField() {
        assertEquals(Optional.of("RS256"), json.algorithm("{\"kid\":{\"x\":[1]},\"alg\":\"RS256\"}"));
        assertEquals(Optional.empty(), json.algorithm("{\"alg\":256}"));
        assertEquals(Optional.empty(), json.algorithm("{\"typ\":\"JWT\"}"));
    }

    private static MalformedJwtException assertMalformed(
            org.junit.jupiter.api.function.Executable action, JwtPart part, MalformedJwtException.Reason reason) {
        MalformedJwtException error = assertThrows(MalformedJwtException.class, action);
        assertEquals(part, error.part());
        assertEquals(reason, error.reason());
        return error;
    }
}
