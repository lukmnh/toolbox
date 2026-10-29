package io.github.lukmnh.toolbox.devutils.regex.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.text.RegexTimeoutException;
import io.github.lukmnh.toolbox.devutils.regex.domain.EngineKind;
import io.github.lukmnh.toolbox.devutils.regex.domain.EnginePattern;
import io.github.lukmnh.toolbox.devutils.regex.domain.GroupMatch;
import io.github.lukmnh.toolbox.devutils.regex.domain.MatchScan;
import io.github.lukmnh.toolbox.devutils.regex.domain.OffsetMap;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexCompileException;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexMatch;
import io.github.lukmnh.toolbox.devutils.regex.domain.ScanLimits;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Ekspektasi diambil dari {@code regexp.FindAllStringSubmatchIndex} Go 1.26 (offset byte UTF-8 dikonversi ke indeks
 * karakter).
 */
class Re2jRegexEngineTest {

    private static final ScanLimits PLENTY = new ScanLimits(100, 100, Duration.ofSeconds(2));

    private final Re2jRegexEngine engine = new Re2jRegexEngine();

    private static EnginePattern pattern(String source, int groupCount) {
        List<String> names = new ArrayList<>(Collections.nCopies(groupCount + 1, ""));
        return new EnginePattern(EngineKind.RE2, source, 0, names, OffsetMap.identity(source.length()), false);
    }

    private List<List<Integer>> spans(String source, String text) {
        MatchScan scan = engine.compile(pattern(source, 0)).scan(text, PLENTY);
        return scan.matches().stream()
                .map(match -> List.of(match.start(), match.end()))
                .toList();
    }

    @Test
    void emptyMatchRightAfterPreviousMatchIsSkippedLikeGo() {
        // Go: [[0 0] [1 4] [5 5]]; RE2/J find() sendiri juga mengembalikan [4,4]
        assertEquals(List.of(List.of(0, 0), List.of(1, 4), List.of(5, 5)), spans("a*", "baaac"));
        // Go (byte): [[0 2] [3 3]] → karakter: [0,1] [2,2]
        assertEquals(List.of(List.of(0, 1), List.of(2, 2)), spans("é|", "éa"));
    }

    @Test
    void emptyMatchesStepByCodePoint() {
        // Go (byte): [0 1 3 4 5 6] → karakter 0..5
        assertEquals(6, spans("x*", "héllo").size());
        assertEquals(List.of(List.of(0, 0), List.of(2, 2), List.of(3, 3), List.of(5, 5)), spans("\\b", "ab cd"));
        assertEquals(List.of(List.of(0, 1), List.of(3, 4)), spans("\\b\\w", "ab cd"));
        assertEquals(List.of(List.of(0, 0), List.of(2, 2), List.of(4, 4)), spans("(?m)^", "a\nb\n"));
    }

    @Test
    void namedGroupsAndUnmatchedAlternatives() {
        EnginePattern named = new EnginePattern(
                EngineKind.RE2, "(?P<w>\\w+)|(\\d)", 0, List.of("", "w", ""), OffsetMap.identity(15), false);
        MatchScan scan = engine.compile(named).scan("ab 12", PLENTY);
        assertEquals(2, scan.totalCount());
        RegexMatch second = scan.matches().get(1);
        assertEquals(new GroupMatch(1, "w", 3, 5, "12"), second.groups().get(0));
        assertFalse(second.groups().get(1).matched());
        assertEquals(2, engine.compile(named).groupCount());
    }

    @Test
    void countLimitStopsScanning() {
        MatchScan scan = engine.compile(pattern("a", 0)).scan("aaaa", new ScanLimits(1, 2, Duration.ofSeconds(2)));
        assertEquals(1, scan.matches().size());
        assertEquals(2, scan.totalCount());
        assertFalse(scan.countComplete());
        assertTrue(engine.compile(pattern("a", 0))
                .scan("aa", new ScanLimits(1, 2, Duration.ofSeconds(2)))
                .countComplete());
    }

    @Test
    void compileErrorPositionComesFromQuotedFragment() {
        RegexCompileException unsupported =
                assertThrows(RegexCompileException.class, () -> engine.compile(pattern("a(?=b)", 0)));
        assertEquals("invalid or unsupported Perl syntax", unsupported.description());
        assertEquals(1, unsupported.position());

        // Prefix flag Go "(?i)" dipetakan kembali ke pola asli "(ab"
        EnginePattern prefixed =
                new EnginePattern(EngineKind.RE2, "(?i)(ab", 0, List.of("", ""), OffsetMap.withPrefix(4, 3), false);
        RegexCompileException unclosed = assertThrows(RegexCompileException.class, () -> engine.compile(prefixed));
        assertEquals("missing closing )", unclosed.description());
        assertEquals(0, unclosed.position());

        assertEquals(
                2,
                assertThrows(RegexCompileException.class, () -> engine.compile(pattern("ab\\Z", 0)))
                        .position());
    }

    @Test
    void timeoutAppliesToLongScans() {
        String text = "x".repeat(200_000);
        assertThrows(
                RegexTimeoutException.class,
                () -> engine.compile(pattern("y", 0)).scan(text, new ScanLimits(10, 10, Duration.ofNanos(1))));
        assertEquals(EngineKind.RE2, engine.kind());
    }
}
