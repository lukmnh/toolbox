package io.github.lukmnh.toolbox.devutils.cron.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class CronSnippetTest {

    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");

    @Test
    void snippetPerTarget() {
        assertEquals(
                "*/15 8-17 * * 1-5 /usr/local/bin/job.sh",
                CronSnippet.of(CronTarget.UNIX, "*/15 8-17 * * 1-5", JAKARTA, false));
        assertEquals(
                "@Scheduled(cron = \"0 */15 8-17 * * MON-FRI\", zone = \"Asia/Jakarta\")",
                CronSnippet.of(CronTarget.SPRING, "0 */15 8-17 * * MON-FRI", JAKARTA, true));
        assertEquals(
                "@Scheduled(cron = \"0 0/15 8-17 ? * MON-FRI\", timeZone = \"Asia/Jakarta\")",
                CronSnippet.of(CronTarget.QUARKUS, "0 0/15 8-17 ? * MON-FRI", JAKARTA, true));
        assertEquals(
                "Schedule::command('app:job')->cron('*/15 8-17 * * 1-5')->timezone('Asia/Jakarta');",
                CronSnippet.of(CronTarget.LARAVEL, "*/15 8-17 * * 1-5", JAKARTA, false));
    }

    @Test
    void goSnippetAddsTimeZoneAndSecondsOption() {
        assertEquals(
                "c := cron.New()\nc.AddFunc(\"CRON_TZ=Asia/Jakarta */15 8-17 * * 1-5\", job)",
                CronSnippet.of(CronTarget.GO, "*/15 8-17 * * 1-5", JAKARTA, false));
        assertEquals(
                "c := cron.New(cron.WithSeconds())\nc.AddFunc(\"CRON_TZ=Asia/Jakarta 30 * * * * *\", job)",
                CronSnippet.of(CronTarget.GO, "30 * * * * *", JAKARTA, true));
        assertEquals(
                "c := cron.New()\nc.AddFunc(\"@every 1h30m\", job)",
                CronSnippet.of(CronTarget.GO, "@every 1h30m", JAKARTA, false));
    }
}
