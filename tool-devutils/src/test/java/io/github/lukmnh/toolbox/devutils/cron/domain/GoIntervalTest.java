package io.github.lukmnh.toolbox.devutils.cron.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GoIntervalTest {

    @Test
    void parsesGoDurations() {
        assertEquals(Duration.ofMinutes(90), GoInterval.parse("1h30m").delay());
        assertEquals(Duration.ofSeconds(90), GoInterval.parse("90s").delay());
        assertEquals(Duration.ofMinutes(90), GoInterval.parse("1.5h").delay());
        assertEquals(Duration.ofSeconds(2), GoInterval.parse("2s999ms").delay());
    }

    @Test
    void belowOneSecondBecomesOneSecondLikeRobfig() {
        assertEquals(Duration.ofSeconds(1), GoInterval.parse("500ms").delay());
        assertEquals(Duration.ofSeconds(1), GoInterval.parse("0").delay());
        assertEquals(Duration.ofSeconds(1), GoInterval.parse("-5m").delay());
        assertEquals(Duration.ofSeconds(1), GoInterval.parse("999999µs").delay());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1", "h", "1x", "1.2.3s", "-", "1h 30m", "5d"})
    void invalidDurationsAreRejected(String text) {
        InvalidCronException error = assertThrows(InvalidCronException.class, () -> GoInterval.parse(text));
        assertEquals(InvalidCronException.Reason.INVALID_INTERVAL, error.reason());
        assertEquals(text, error.token());
    }

    @Test
    void nextRunsStartOneDelayAfterNowTruncatedToSecond() {
        ZonedDateTime now = ZonedDateTime.of(2026, 10, 29, 10, 0, 5, 700_000_000, ZoneId.of("Asia/Jakarta"));
        List<ZonedDateTime> runs = GoInterval.parse("90m").nextRuns(now, 3);
        assertEquals(
                List.of(
                        now.withNano(0).plusMinutes(90),
                        now.withNano(0).plusMinutes(180),
                        now.withNano(0).plusMinutes(270)),
                runs);
    }
}
