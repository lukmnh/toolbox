package io.github.lukmnh.toolbox.devutils.jwt.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.devutils.jwt.testsupport.TestTokens;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class JwtDecoderTest {

    @Test
    void decodesRfc7519Example() {
        RawJwt jwt = JwtDecoder.decode(TestTokens.RFC_TOKEN);

        assertEquals("{\"typ\":\"JWT\",\r\n \"alg\":\"HS256\"}", jwt.headerJson());
        assertTrue(jwt.payloadJson().contains("\"iss\":\"joe\""));
        assertEquals("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk", jwt.signatureText());
        assertEquals(32, jwt.signature().length);
        String[] parts = TestTokens.RFC_TOKEN.split("\\.");
        assertArrayEquals((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII), jwt.signingInput());
    }

    @Test
    void bearerPrefixAndWhitespaceAreRemoved() {
        String pasted = "  Bearer " + TestTokens.RFC_TOKEN.substring(0, 20) + "\n  "
                + TestTokens.RFC_TOKEN.substring(20) + "\n";

        assertEquals(TestTokens.RFC_TOKEN, JwtDecoder.normalize(pasted));
        assertEquals(
                "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk",
                JwtDecoder.decode(pasted).signatureText());
    }

    @Test
    void fivePartTokenIsReportedAsJwe() {
        assertThrows(JweNotSupportedException.class, () -> JwtDecoder.decode("a.b.c.d.e"));
    }

    @Test
    void emptyTokenAndWrongPartCountPointAtToken() {
        assertMalformed("   ", JwtPart.TOKEN, MalformedJwtException.Reason.EMPTY_TOKEN);
        MalformedJwtException error =
                assertMalformed("abc.def", JwtPart.TOKEN, MalformedJwtException.Reason.WRONG_PART_COUNT);
        assertEquals("2", error.detail());
    }

    @Test
    void brokenPartIsIdentified() {
        String[] parts = TestTokens.RFC_TOKEN.split("\\.");
        assertMalformed("." + parts[1] + "." + parts[2], JwtPart.HEADER, MalformedJwtException.Reason.EMPTY_PART);
        assertMalformed(
                "e$$." + parts[1] + "." + parts[2], JwtPart.HEADER, MalformedJwtException.Reason.INVALID_BASE64URL);
        assertMalformed(
                parts[0] + ".a+b/." + parts[2], JwtPart.PAYLOAD, MalformedJwtException.Reason.INVALID_BASE64URL);
        assertMalformed(
                parts[0] + "." + parts[1] + ".ab+/", JwtPart.SIGNATURE, MalformedJwtException.Reason.INVALID_BASE64URL);
        String invalidUtf8 = TestTokens.base64Url(new byte[] {(byte) 0xff, (byte) 0xfe});
        assertMalformed(
                invalidUtf8 + "." + parts[1] + "." + parts[2], JwtPart.HEADER, MalformedJwtException.Reason.NOT_UTF8);
    }

    @Test
    void emptySignatureIsAllowedForUnsecuredTokens() {
        RawJwt jwt = JwtDecoder.decode(TestTokens.unsigned(TestTokens.header("none"), "{}") + ".");

        assertEquals(0, jwt.signature().length);
    }

    private static MalformedJwtException assertMalformed(
            String token, JwtPart part, MalformedJwtException.Reason reason) {
        MalformedJwtException error = assertThrows(MalformedJwtException.class, () -> JwtDecoder.decode(token));
        assertEquals(part, error.part());
        assertEquals(reason, error.reason());
        return error;
    }
}
