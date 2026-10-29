package io.github.lukmnh.toolbox.common.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.task.TaskCancelledException;
import java.time.Duration;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class TimedRegexTest {

    private static final Duration SHORT = Duration.ofMillis(200);

    /**
     * {@code (a+)+$} dari spec tidak lagi eksponensial di JDK 21 (ada memoization); backreference mematikan optimasi
     * itu sehingga pola ini tetap catastrophic.
     */
    private static final Pattern EVIL = Pattern.compile("(a+)+\\1b");

    private static final String EVIL_INPUT = "a".repeat(40) + "!";

    @Test
    void catastrophicBacktrackingTimesOut() {
        long started = System.nanoTime();
        assertThrows(
                RegexTimeoutException.class,
                () -> TimedRegex.run(
                        EVIL_INPUT, SHORT, text -> EVIL.matcher(text).find()));
        assertTrue(Duration.ofNanos(System.nanoTime() - started).compareTo(Duration.ofSeconds(2)) < 0);
    }

    @Test
    void normalRegexCompletes() {
        boolean found = TimedRegex.run(
                "halo dunia",
                SHORT,
                text -> Pattern.compile("dun.a").matcher(text).find());
        assertTrue(found);
    }

    @Test
    void interruptedThreadIsTreatedAsCancellation() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(
                    TaskCancelledException.class,
                    () -> TimedRegex.run(
                            EVIL_INPUT,
                            Duration.ofSeconds(5),
                            text -> EVIL.matcher(text).find()));
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void timeoutIsReported() {
        RegexTimeoutException error = assertThrows(
                RegexTimeoutException.class,
                () -> TimedRegex.run(
                        EVIL_INPUT, SHORT, text -> EVIL.matcher(text).find()));
        assertEquals(SHORT, error.timeout());
    }
}
