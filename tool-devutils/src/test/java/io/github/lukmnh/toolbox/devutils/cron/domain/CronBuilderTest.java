package io.github.lukmnh.toolbox.devutils.cron.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class CronBuilderTest {

    private static final LocalTime EIGHT_THIRTY = LocalTime.of(8, 30);

    private static String build(CronPreset preset, CronTarget target) {
        return CronBuilder.build(preset, target, false);
    }

    @Test
    void everyNMinutesPerTarget() {
        CronPreset preset = new CronPreset.EveryMinutes(15);
        assertEquals("*/15 * * * *", build(preset, CronTarget.UNIX));
        assertEquals("0 */15 * * * *", build(preset, CronTarget.SPRING));
        assertEquals("0 */15 * * * ?", build(preset, CronTarget.QUARKUS));
        assertEquals("*/15 * * * *", build(preset, CronTarget.GO));
        assertEquals("0 */15 * * * *", CronBuilder.build(preset, CronTarget.GO, true));
        assertEquals("* * * * *", build(new CronPreset.EveryMinutes(1), CronTarget.LARAVEL));
    }

    @Test
    void weekdaysUseNumbersOrNames() {
        CronPreset preset = new CronPreset.Weekdays(EIGHT_THIRTY);
        assertEquals("30 8 * * 1-5", build(preset, CronTarget.UNIX));
        assertEquals("0 30 8 * * MON-FRI", build(preset, CronTarget.SPRING));
        assertEquals("0 30 8 ? * MON-FRI", build(preset, CronTarget.QUARKUS));
        assertEquals("30 8 * * 1-5", build(preset, CronTarget.LARAVEL));
    }

    @Test
    void weeklySundayIsZeroForFiveFieldTargets() {
        CronPreset preset = new CronPreset.Weekly(DayOfWeek.SUNDAY, EIGHT_THIRTY);
        assertEquals("30 8 * * 0", build(preset, CronTarget.GO));
        assertEquals("0 30 8 ? * SUN", build(preset, CronTarget.QUARKUS));
    }

    @Test
    void hourlyDailyMonthly() {
        assertEquals("5 * * * *", build(new CronPreset.Hourly(5), CronTarget.UNIX));
        assertEquals("0 0 9 * * *", build(new CronPreset.Daily(LocalTime.of(9, 0)), CronTarget.SPRING));
        assertEquals("0 0 9 15 * ?", build(new CronPreset.Monthly(15, LocalTime.of(9, 0)), CronTarget.QUARKUS));
    }

    @Test
    void customBlankFieldsBecomeWildcard() {
        CronPreset preset = new CronPreset.Custom(" 0 ", "9-17", "", "JAN", "MON");
        assertEquals("0 9-17 * JAN MON", build(preset, CronTarget.UNIX));
        assertEquals("0 0 9-17 ? JAN MON", build(preset, CronTarget.QUARKUS));
    }

    @Test
    void presetRangesAreValidated() {
        assertThrows(IllegalArgumentException.class, () -> new CronPreset.EveryMinutes(0));
        assertThrows(IllegalArgumentException.class, () -> new CronPreset.Hourly(60));
        assertThrows(IllegalArgumentException.class, () -> new CronPreset.Monthly(32, EIGHT_THIRTY));
    }
}
