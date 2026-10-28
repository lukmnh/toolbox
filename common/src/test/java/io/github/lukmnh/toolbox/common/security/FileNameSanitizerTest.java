package io.github.lukmnh.toolbox.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FileNameSanitizerTest {

    @ParameterizedTest
    @CsvSource(
            delimiterString = "=>",
            value = {
                "../a => a",
                "..\\..\\etc\\passwd => etcpasswd",
                "a:b*c?.pdf => abc.pdf",
                "CON.pdf => _CON.pdf",
                "nul.tar.gz => _nul.tar.gz",
                "com1 => _com1",
                "CONSOLE.pdf => CONSOLE.pdf",
                "'  .tersembunyi. ' => tersembunyi",
                "laporan_edited.pdf => laporan_edited.pdf",
                "'...' => hasil",
                "'' => hasil"
            })
    void sanitizesDangerousNames(String input, String expected) {
        assertEquals(expected, FileNameSanitizer.sanitize(input));
    }

    @Test
    void controlCharactersAreRemoved() {
        assertEquals("ab.pdf", FileNameSanitizer.sanitize("a\u0000\nb.pdf"));
    }

    @Test
    void longNameIsTruncatedKeepingExtension() {
        String sanitized = FileNameSanitizer.sanitize("x".repeat(500) + ".pdf");

        assertEquals(FileNameSanitizer.MAX_LENGTH, sanitized.length());
        assertTrue(sanitized.endsWith(".pdf"));
    }

    @Test
    void truncationDoesNotSplitSurrogatePair() {
        String emoji = "😀";
        String sanitized = FileNameSanitizer.sanitize("a" + emoji.repeat(300));

        assertTrue(sanitized.length() <= FileNameSanitizer.MAX_LENGTH);
        assertTrue(!Character.isHighSurrogate(sanitized.charAt(sanitized.length() - 1)));
    }
}
