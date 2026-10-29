package io.github.lukmnh.toolbox.devutils.shared.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class DateTimeTextsTest {

    @Test
    void indonesianShortFormatWithWib() {
        ZonedDateTime time = ZonedDateTime.of(2026, 11, 2, 8, 0, 30, 0, ZoneId.of("Asia/Jakarta"));
        assertEquals("Sen, 2 Nov 2026 08:00 WIB", DateTimeTexts.HUMAN.format(time));
        assertEquals("Sen, 2 Nov 2026 08:00:30 WIB", DateTimeTexts.HUMAN_WITH_SECONDS.format(time));
    }
}
