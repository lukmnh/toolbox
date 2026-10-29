package io.github.lukmnh.toolbox.devutils.regex.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.task.TaskCancelledException;
import io.github.lukmnh.toolbox.common.text.RegexTimeoutException;
import io.github.lukmnh.toolbox.devutils.regex.domain.EngineKind;
import io.github.lukmnh.toolbox.devutils.regex.domain.EnginePattern;
import io.github.lukmnh.toolbox.devutils.regex.domain.GroupMatch;
import io.github.lukmnh.toolbox.devutils.regex.domain.MatchScan;
import io.github.lukmnh.toolbox.devutils.regex.domain.OffsetMap;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexCompileException;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexMatch;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexTooComplexException;
import io.github.lukmnh.toolbox.devutils.regex.domain.ScanLimits;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class JdkRegexEngineTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);
    private static final ScanLimits PLENTY = new ScanLimits(100, 100, TIMEOUT);

    private final JdkRegexEngine engine = new JdkRegexEngine();

    private static EnginePattern pattern(String source, List<String> groupNames) {
        return new EnginePattern(EngineKind.JDK, source, 0, groupNames, OffsetMap.identity(source.length()), false);
    }

    private MatchScan scan(String source, List<String> groupNames, String text, ScanLimits limits) {
        return engine.compile(pattern(source, groupNames)).scan(text, limits);
    }

    @Test
    void namedAndNumberedGroupsAreReported() {
        MatchScan scan = scan("(?<year>\\d{4})-(\\d{2})", List.of("", "year", ""), "lahir 1990-07, 2001-12", PLENTY);
        assertEquals(2, scan.totalCount());
        RegexMatch first = scan.matches().getFirst();
        assertEquals(6, first.start());
        assertEquals("1990-07", first.value());
        assertEquals(new GroupMatch(1, "year", 6, 10, "1990"), first.groups().get(0));
        assertEquals(new GroupMatch(2, "", 11, 13, "07"), first.groups().get(1));
    }

    @Test
    void unmatchedGroupIsMarked() {
        MatchScan scan = scan("(a)|(b)", List.of("", "", ""), "b", PLENTY);
        GroupMatch first = scan.matches().getFirst().groups().getFirst();
        assertFalse(first.matched());
        assertEquals("", first.value());
        assertTrue(scan.matches().getFirst().groups().get(1).matched());
    }

    @Test
    void detailIsLimitedButCountingContinues() {
        MatchScan limited = scan("a", List.of(""), "aaaaa", new ScanLimits(2, 3, TIMEOUT));
        assertEquals(2, limited.matches().size());
        assertEquals(3, limited.totalCount());
        assertFalse(limited.countComplete());

        MatchScan complete = scan("a", List.of(""), "aaaaa", new ScanLimits(2, 10, TIMEOUT));
        assertEquals(5, complete.totalCount());
        assertTrue(complete.countComplete());

        MatchScan exact = scan("a", List.of(""), "aaa", new ScanLimits(3, 3, TIMEOUT));
        assertTrue(exact.countComplete());
    }

    @Test
    void emptyMatchesAdvance() {
        MatchScan scan = scan("x*", List.of(""), "ab", PLENTY);
        assertEquals(3, scan.totalCount());
    }

    @Test
    void catastrophicBacktrackingTimesOut() {
        // (a+)+$ sudah dioptimasi JDK 21; backreference mematikan memoization
        EnginePattern evil = pattern("(a+)+\\1b", List.of("", ""));
        assertThrows(
                RegexTimeoutException.class,
                () -> engine.compile(evil).scan("a".repeat(40) + "!", new ScanLimits(10, 10, Duration.ofMillis(200))));
    }

    @Test
    void interruptedThreadCancels() {
        EnginePattern evil = pattern("(a+)+\\1b", List.of("", ""));
        Thread.currentThread().interrupt();
        try {
            assertThrows(
                    TaskCancelledException.class,
                    () -> engine.compile(evil)
                            .scan("a".repeat(40) + "!", new ScanLimits(10, 10, Duration.ofSeconds(30))));
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void stackOverflowBecomesTooComplex() throws InterruptedException {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        // Stack kecil supaya overflow terjadi deterministik, tidak bergantung ukuran stack default JVM
        Thread worker = new Thread(
                null,
                () -> {
                    try {
                        scan("(a|b)*", List.of("", ""), "ab".repeat(100_000), new ScanLimits(1, 1, TIMEOUT));
                    } catch (RuntimeException e) {
                        failure.set(e);
                    }
                },
                "regex-small-stack",
                256 * 1024);
        worker.start();
        worker.join();
        assertInstanceOf(RegexTooComplexException.class, failure.get());
    }

    @Test
    void compileErrorPositionIsMappedToOriginalPattern() {
        RegexCompileException direct =
                assertThrows(RegexCompileException.class, () -> engine.compile(pattern("ab)", List.of(""))));
        assertEquals(1, direct.position());
        assertEquals("Unmatched closing ')'", direct.description());

        // Pola engine "XXXab)" berasal dari pola asli "ab)" dengan prefix 3 karakter
        EnginePattern prefixed =
                new EnginePattern(EngineKind.JDK, "XXXab)", 0, List.of(""), OffsetMap.withPrefix(3, 3), false);
        assertEquals(
                1,
                assertThrows(RegexCompileException.class, () -> engine.compile(prefixed))
                        .position());
    }

    @Test
    void invalidFlagsAreReportedWithoutPosition() {
        EnginePattern badFlags =
                new EnginePattern(EngineKind.JDK, "a", 1 << 30, List.of(""), OffsetMap.identity(1), false);
        RegexCompileException error = assertThrows(RegexCompileException.class, () -> engine.compile(badFlags));
        assertEquals(RegexCompileException.UNKNOWN_POSITION, error.position());
    }

    @Test
    void flagsAreApplied() {
        EnginePattern caseless = new EnginePattern(
                EngineKind.JDK, "abc", Pattern.CASE_INSENSITIVE, List.of(""), OffsetMap.identity(3), false);
        assertEquals(1, engine.compile(caseless).scan("ABC", PLENTY).totalCount());
        assertEquals(EngineKind.JDK, engine.kind());
        assertEquals(2, engine.compile(pattern("(a)(b)", List.of("", "", ""))).groupCount());
    }
}
