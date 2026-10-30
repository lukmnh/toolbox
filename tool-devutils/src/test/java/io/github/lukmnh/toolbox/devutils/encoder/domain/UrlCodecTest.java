package io.github.lukmnh.toolbox.devutils.encoder.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UrlCodecTest {

    @Test
    void spacesFollowChosenStyle() {
        assertEquals("a%20b%2Bc", UrlCodec.encodeComponent("a b+c", SpaceEncoding.PERCENT_20));
        assertEquals("a+b%2Bc", UrlCodec.encodeComponent("a b+c", SpaceEncoding.PLUS));
    }

    @Test
    void componentEncodingIsUtf8AndKeepsTilde() {
        assertEquals("caf%C3%A9~%26%3D%2F", UrlCodec.encodeComponent("café~&=/", SpaceEncoding.PERCENT_20));
        assertEquals("%F0%9F%98%80", UrlCodec.encodeComponent("😀", SpaceEncoding.PERCENT_20));
    }

    @Test
    void plusIsSpaceOnlyWhenRequested() {
        assertEquals("a b c", UrlCodec.decodeComponent("a+b%20c", true));
        assertEquals("a+b c", UrlCodec.decodeComponent("a+b%20c", false));
    }

    @Test
    void decodeIsTolerantToAlreadyDecodedCharacters() {
        assertEquals("café 😀", UrlCodec.decodeComponent("café%20%F0%9F%98%80", false));
        assertEquals("�", UrlCodec.decodeComponent("%C3", false));
    }

    @ParameterizedTest
    @CsvSource(
            value = {"abc%G1 => 3 => %G1", "abc% => 3 => %", "a%2 => 1 => %2", "%%41 => 0 => %%4"},
            delimiterString = "=>")
    void brokenPercentReportsPosition(String input, int position, String sequence) {
        InvalidPercentEncodingException error =
                assertThrows(InvalidPercentEncodingException.class, () -> UrlCodec.decodeComponent(input, false));

        assertEquals(position, error.position());
        assertEquals(sequence, error.sequence());
    }

    @Test
    void fullUrlEncodingKeepsStructureAndExistingEscapes() {
        assertEquals(
                "https://x.id/a%20b/caf%C3%A9?q=1&r=%2F#top",
                UrlCodec.encodeFullUrl("https://x.id/a b/café?q=1&r=%2F#top"));
        assertEquals("100%25", UrlCodec.encodeFullUrl("100%"));
    }
}
