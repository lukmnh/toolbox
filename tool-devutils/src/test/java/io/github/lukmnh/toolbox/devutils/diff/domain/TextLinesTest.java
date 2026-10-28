package io.github.lukmnh.toolbox.devutils.diff.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextLinesTest {

    @Test
    void emptyTextHasNoLines() {
        TextLines lines = TextLines.of("");
        assertEquals(List.of(), lines.lines());
        assertFalse(lines.endsWithNewline());
    }

    @Test
    void trailingNewlineIsTrackedSeparately() {
        assertEquals(new TextLines(List.of("a", "b"), true), TextLines.of("a\nb\n"));
        assertEquals(new TextLines(List.of("a", "b"), false), TextLines.of("a\nb"));
        assertEquals(new TextLines(List.of("a", "", "b"), false), TextLines.of("a\n\nb"));
    }

    @Test
    void carriageReturnStaysOnTheLine() {
        TextLines lines = TextLines.of("a\r\nb\r\n");
        assertEquals(List.of("a\r", "b\r"), lines.lines());
        assertTrue(lines.endsWithNewline());
    }
}
