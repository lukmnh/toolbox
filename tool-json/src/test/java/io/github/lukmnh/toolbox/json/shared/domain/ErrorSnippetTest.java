package io.github.lukmnh.toolbox.json.shared.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ErrorSnippetTest {

    @Test
    void cutsAroundPositionAndKeepsMarkerOnErrorCharacter() {
        String text = "a".repeat(100) + "X" + "b".repeat(100);

        ErrorSnippet snippet = ErrorSnippet.around(text, 100, 40);

        assertEquals(80, snippet.text().length());
        assertEquals('X', snippet.text().charAt(snippet.marker()));
    }

    @Test
    void clampsAtStartAndEndOfText() {
        ErrorSnippet start = ErrorSnippet.around("{bad", 0, 40);
        ErrorSnippet end = ErrorSnippet.around("[1,", 3, 40);

        assertEquals(0, start.marker());
        assertEquals(3, end.marker());
        assertEquals("[1,", end.text());
    }

    @Test
    void newlinesAndTabsBecomeSpacesSoMarkerStaysAligned() {
        ErrorSnippet snippet = ErrorSnippet.around("{\n\t\"a\": }", 8, 40);

        assertEquals("{  \"a\": }", snippet.text());
        assertEquals('}', snippet.text().charAt(snippet.marker()));
    }

    @Test
    void utf8WindowSkipsCutCharacterAndCountsMarkerInCharacters() {
        byte[] full = "é😀x!".getBytes(StandardCharsets.UTF_8);
        // Jendela dimulai di tengah karakter é (byte kedua)
        byte[] window = java.util.Arrays.copyOfRange(full, 1, full.length);
        int markerByte = window.length - 1;

        ErrorSnippet snippet = ErrorSnippet.aroundUtf8(window, markerByte, 40);

        assertEquals("😀x!", snippet.text());
        assertEquals('!', snippet.text().charAt(snippet.marker()));
    }

    @Test
    void decodePrefixNeverSplitsMultiByteCharacter() {
        byte[] bytes = "ab😀".getBytes(StandardCharsets.UTF_8);

        assertEquals("ab", Utf8Text.decodePrefix(bytes, 4));
        assertEquals("ab😀", Utf8Text.decodePrefix(bytes, 6));
    }
}
