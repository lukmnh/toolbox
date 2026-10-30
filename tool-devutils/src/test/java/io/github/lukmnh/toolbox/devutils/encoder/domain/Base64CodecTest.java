package io.github.lukmnh.toolbox.devutils.encoder.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class Base64CodecTest {

    private static final Base64Options STANDARD = Base64Options.DEFAULT;

    /** Test vector RFC 4648 bagian 10. */
    @ParameterizedTest
    @CsvSource(
            value = {
                "'' => ''",
                "f => Zg==",
                "fo => Zm8=",
                "foo => Zm9v",
                "foob => Zm9vYg==",
                "fooba => Zm9vYmE=",
                "foobar => Zm9vYmFy"
            },
            delimiterString = "=>")
    void rfc4648VectorsRoundTrip(String plain, String encoded) {
        assertEquals(encoded, Base64Codec.encode(plain, STANDARD).output());
        assertEquals(plain, Base64Codec.decode(encoded, STANDARD).output());
    }

    @Test
    void urlSafeVariantUsesDashAndUnderscore() {
        String text = new String(new byte[] {(byte) 0xFB, (byte) 0xFF}, StandardCharsets.ISO_8859_1);
        Base64Options urlSafe = new Base64Options(Base64Variant.URL_SAFE, true, TextCharset.ISO_8859_1);
        Base64Options standard = new Base64Options(Base64Variant.STANDARD, true, TextCharset.ISO_8859_1);

        assertEquals("-_8=", Base64Codec.encode(text, urlSafe).output());
        assertEquals("+/8=", Base64Codec.encode(text, standard).output());
        assertEquals(text, Base64Codec.decode("-_8=", standard).output());
    }

    @Test
    void paddingCanBeOmitted() {
        Base64Options noPadding = new Base64Options(Base64Variant.STANDARD, false, TextCharset.UTF_8);

        assertEquals("Zg", Base64Codec.encode("f", noPadding).output());
        assertEquals("f", Base64Codec.decode("Zg", STANDARD).output());
    }

    @Test
    void mimeVariantWrapsAt76Columns() {
        Base64Options mime = new Base64Options(Base64Variant.MIME, true, TextCharset.UTF_8);

        String encoded = Base64Codec.encode("a".repeat(100), mime).output();

        String[] lines = encoded.split("\r\n");
        assertEquals(2, lines.length);
        assertEquals(76, lines[0].length());
        assertEquals("a".repeat(100), Base64Codec.decode(encoded, STANDARD).output());
    }

    @Test
    void decodeIgnoresWhitespaceAndDataUriPrefix() {
        assertEquals(
                "foobar", Base64Codec.decode("  Zm9v\n  YmFy\r\n", STANDARD).output());
        assertEquals(
                "foobar",
                Base64Codec.decode("data:text/plain;base64,Zm9vYmFy", STANDARD).output());
        assertEquals(
                "foobar",
                Base64Codec.decode("\n DATA:text/plain;base64,Zm9v YmFy", STANDARD)
                        .output());
    }

    @Test
    void invalidCharacterReportsPositionInOriginalInput() {
        InvalidBase64Exception error =
                assertThrows(InvalidBase64Exception.class, () -> Base64Codec.decode("Zm9v\n Ym!Fy", STANDARD));

        assertEquals(InvalidBase64Exception.Reason.INVALID_CHARACTER, error.reason());
        assertEquals(8, error.position());
        assertEquals('!', error.character());
    }

    @Test
    void dataAfterPaddingIsRejected() {
        InvalidBase64Exception error =
                assertThrows(InvalidBase64Exception.class, () -> Base64Codec.decode("Zg==Zg", STANDARD));

        assertEquals(InvalidBase64Exception.Reason.MISPLACED_PADDING, error.reason());
        assertEquals(4, error.position());
    }

    @Test
    void singleDanglingCharacterIsTruncated() {
        InvalidBase64Exception error =
                assertThrows(InvalidBase64Exception.class, () -> Base64Codec.decode("Zm9vY", STANDARD));

        assertEquals(InvalidBase64Exception.Reason.TRUNCATED, error.reason());
        assertEquals(4, error.position());
    }

    @Test
    void charsetIsAppliedBothWays() {
        Base64Options utf16 = new Base64Options(Base64Variant.STANDARD, true, TextCharset.UTF_16);

        String encoded = Base64Codec.encode("héllo", utf16).output();

        assertEquals("héllo", Base64Codec.decode(encoded, utf16).output());
        assertTrue(encoded.length()
                > Base64Codec.encode("héllo", STANDARD).output().length());
    }

    @Test
    void conversionReportsCharacterAndByteLengths() {
        TextConversion conversion = Base64Codec.encode("é", STANDARD);

        assertEquals(1, conversion.inputChars());
        assertEquals(2, conversion.inputBytes());
        assertEquals(4, conversion.outputChars());
        assertArrayEquals(new byte[] {(byte) 0xC3, (byte) 0xA9}, Base64Codec.decodeBytes(conversion.output()));
    }
}
