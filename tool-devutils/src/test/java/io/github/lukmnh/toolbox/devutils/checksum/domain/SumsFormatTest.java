package io.github.lukmnh.toolbox.devutils.checksum.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class SumsFormatTest {

    private static final String SHA256_ABC = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
    private static final String MD5_ABC = "900150983cd24fb0d6963f7d28e17f72";

    @Test
    void parsesTextAndBinaryModeLines() {
        List<SumsEntry> entries =
                SumsFormat.parse(SHA256_ABC + "  abc.txt\n" + MD5_ABC.toUpperCase() + " *data/file name.bin\n");
        assertEquals(
                List.of(new SumsEntry(SHA256_ABC, "abc.txt"), new SumsEntry(MD5_ABC, "data/file name.bin")), entries);
    }

    @Test
    void skipsBlankLinesCommentsBomAndCarriageReturns() {
        String text = "﻿# dibuat oleh toolbox\r\n\r\n" + SHA256_ABC + "  abc.txt\r\n";
        assertEquals(List.of(new SumsEntry(SHA256_ABC, "abc.txt")), SumsFormat.parse(text));
    }

    @Test
    void writeThenParseRoundTrips() {
        List<SumsEntry> entries = List.of(new SumsEntry(SHA256_ABC, "a.txt"), new SumsEntry(MD5_ABC, "b c.txt"));
        String written = SumsFormat.write(entries);
        assertEquals(SHA256_ABC + "  a.txt\n" + MD5_ABC + "  b c.txt\n", written);
        assertEquals(entries, SumsFormat.parse(written));
    }

    @Test
    void malformedLineReportsLineNumber() {
        String text = SHA256_ABC + "  ok.txt\n\nini bukan checksum\n";
        InvalidSumsFileException error = assertThrows(InvalidSumsFileException.class, () -> SumsFormat.parse(text));
        assertEquals(3, error.lineNumber());
    }

    @Test
    void unknownHashLengthOrMissingNameIsMalformed() {
        assertEquals(
                1,
                assertThrows(InvalidSumsFileException.class, () -> SumsFormat.parse("abcd  a.txt"))
                        .lineNumber());
        assertEquals(
                1,
                assertThrows(InvalidSumsFileException.class, () -> SumsFormat.parse(SHA256_ABC + "  "))
                        .lineNumber());
        assertEquals(
                1,
                assertThrows(InvalidSumsFileException.class, () -> SumsFormat.parse(SHA256_ABC + " xa.txt"))
                        .lineNumber());
    }

    @Test
    void fileWithoutEntriesIsRejected() {
        InvalidSumsFileException error =
                assertThrows(InvalidSumsFileException.class, () -> SumsFormat.parse("# kosong\n\n"));
        assertEquals(InvalidSumsFileException.NO_ENTRIES, error.lineNumber());
    }
}
