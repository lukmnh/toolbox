package io.github.lukmnh.toolbox.pdf.shared.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lukmnh.toolbox.pdf.shared.domain.InvalidPageRangeException.Reason;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PageRangeTest {

    @Test
    void parsesMixedSinglesAndSpansInInputOrder() {
        List<PageRange> ranges = PageRange.parseList("1-3, 5, 8-10", 10);

        assertEquals(List.of(new PageRange(1, 3), PageRange.single(5), new PageRange(8, 10)), ranges);
    }

    @Test
    void toleratesWhitespaceAroundTokensAndDash() {
        assertEquals(List.of(new PageRange(2, 4), PageRange.single(7)), PageRange.parseList("  2 - 4 ,7 ", 7));
    }

    @Test
    void labelsUsedForOutputFileNames() {
        assertEquals("hal5", PageRange.single(5).label());
        assertEquals("hal1-3", new PageRange(1, 3).label());
    }

    @Test
    void eachPageProducesOneRangePerPage() {
        assertEquals(List.of(PageRange.single(1), PageRange.single(2), PageRange.single(3)), PageRange.eachPage(3));
    }

    @ParameterizedTest(name = "\"{0}\" → {1}")
    @CsvSource(
            delimiter = '|',
            value = {
                "''        | EMPTY",
                "'   '     | EMPTY",
                "abc       | SYNTAX",
                "1-        | SYNTAX",
                "1,,2      | SYNTAX",
                "1-2-3     | SYNTAX",
                "0         | ZERO_OR_NEGATIVE",
                "0-2       | ZERO_OR_NEGATIVE",
                "5-3       | REVERSED",
                "11        | OUT_OF_BOUNDS",
                "9-12      | OUT_OF_BOUNDS",
                "99999999999 | OUT_OF_BOUNDS"
            })
    void rejectsInvalidInputWithSpecificReason(String input, Reason expected) {
        InvalidPageRangeException error =
                assertThrows(InvalidPageRangeException.class, () -> PageRange.parseList(input, 10));

        assertEquals(expected, error.reason());
        assertEquals(10, error.pageCount());
    }

    @Test
    void reportsTheOffendingToken() {
        InvalidPageRangeException error =
                assertThrows(InvalidPageRangeException.class, () -> PageRange.parseList("1-2, 8-3, 4", 10));

        assertEquals("8-3", error.token());
    }
}
