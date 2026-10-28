package io.github.lukmnh.toolbox.devutils.diff.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class LineKeyTest {

    private static DiffOptions options(WhitespaceMode whitespace, boolean ignoreCase, boolean normalize) {
        return new DiffOptions(whitespace, ignoreCase, normalize);
    }

    @Test
    void exactComparisonKeepsEverything() {
        DiffOptions exact = options(WhitespaceMode.EXACT, false, false);
        assertNotEquals(LineKey.of("a b", exact), LineKey.of("a  b", exact));
        assertNotEquals(LineKey.of("A", exact), LineKey.of("a", exact));
        assertNotEquals(LineKey.of("a\r", exact), LineKey.of("a", exact));
    }

    @Test
    void ignoreAllWhitespace() {
        DiffOptions ignoreAll = options(WhitespaceMode.IGNORE_ALL, false, false);
        assertEquals(LineKey.of("a = b\t", ignoreAll), LineKey.of(" a=b", ignoreAll));
    }

    @Test
    void ignoreTrailingWhitespaceOnly() {
        DiffOptions trailing = options(WhitespaceMode.IGNORE_TRAILING, false, false);
        assertEquals(LineKey.of("a b  ", trailing), LineKey.of("a b", trailing));
        assertNotEquals(LineKey.of("  a", trailing), LineKey.of("a", trailing));
    }

    @Test
    void ignoreCaseAndNormalizeLineEndings() {
        DiffOptions options = options(WhitespaceMode.EXACT, true, true);
        assertEquals(LineKey.of("Hello\r", options), LineKey.of("hello", options));
    }

    @Test
    void missingFinalNewlineMakesLastLineDifferent() {
        DiffOptions options = DiffOptions.DEFAULT;
        assertNotEquals(
                LineKey.ofLines(TextLines.of("a\nb"), options), LineKey.ofLines(TextLines.of("a\nb\n"), options));
        assertEquals(LineKey.ofLines(TextLines.of("a\nb"), options), LineKey.ofLines(TextLines.of("a\nb"), options));
    }
}
