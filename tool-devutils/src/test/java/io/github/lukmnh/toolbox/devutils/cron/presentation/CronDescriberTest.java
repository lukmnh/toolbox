package io.github.lukmnh.toolbox.devutils.cron.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.lukmnh.toolbox.devutils.cron.application.ExplainCronUseCase;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronFieldKind;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronRequest;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronTarget;
import io.github.lukmnh.toolbox.devutils.cron.domain.FieldExpr;
import io.github.lukmnh.toolbox.devutils.cron.infrastructure.CronUtilsEngine;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CronDescriberTest {

    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");

    private final ExplainCronUseCase explain = new ExplainCronUseCase(
            new CronUtilsEngine(), Clock.fixed(Instant.parse("2026-10-29T03:00:00Z"), ZoneId.of("UTC")));

    private String describe(String expression, CronTarget target) {
        return CronDescriber.describe(explain.execute(new CronRequest(expression, target, false, JAKARTA))
                .meaning());
    }

    @ParameterizedTest(name = "{0} [{1}]")
    @CsvSource(
            delimiter = '|',
            value = {
                "*/15 8-17 * * 1-5      | UNIX    | Every 15 minutes, from 08:00 to 17:59, on Monday through Friday",
                "0 */15 8-17 * * MON-FRI| SPRING  | Every 15 minutes, from 08:00 to 17:59, on Monday through Friday",
                "0 0 1 * *              | UNIX    | On day 1 at 00:00",
                "0 0 12 ? * MON-FRI     | QUARKUS | On Monday through Friday at 12:00",
                "0 9 * * *              | GO      | Every day at 09:00",
                "0 8,12,17 * * *        | LARAVEL | Every day at 08:00, 12:00, and 17:00",
                "0 0 12 L * *           | SPRING  | On the last day of the month at 12:00",
                "0 0 12 L-3 * ?         | QUARKUS | 3 days before the last day of the month at 12:00",
                "0 0 9 LW * ?           | QUARKUS | On the last weekday of the month at 09:00",
                "0 9 15W * *            | LARAVEL | On the weekday nearest day 15 at 09:00",
                "0 9 * * 5#2            | LARAVEL | On Friday #2 of the month at 09:00",
                "0 0 12 ? * 6L          | QUARKUS | On the last Friday of the month at 12:00",
                "0 0 1 * 1              | UNIX    | On day 1 or on Monday at 00:00",
                "0 0 0 1 * MON          | SPRING  | On day 1, only when it is also on Monday at 00:00",
                "@hourly                | UNIX    | Every hour, on the hour",
                "30 * * * *             | UNIX    | Every hour at minute 30",
                "* * * * *              | UNIX    | Every minute",
                "0 */2 * * *            | GO      | Every 2 hours, on the hour",
                "@yearly                | SPRING  | On day 1 at 00:00, only in January",
                "0 0/15 8-17 ? * MON-FRI 2027 | QUARKUS | Every 15 minutes, from 08:00 to 17:59, on Monday through Friday, only in 2027",
                "0 9 * 1-3 *            | UNIX    | Every day at 09:00, in January through March",
                "15 9-17 * * *          | UNIX    | Every hour at minute 15, from 09:15 to 17:15",
                "0 0 */2 * *            | UNIX    | Every 2 days at 00:00",
                "0 0 * * 0              | UNIX    | On Sunday at 00:00",
            })
    void describesCommonPatterns(String expression, CronTarget target, String expected) {
        assertEquals(expected, describe(expression.strip(), target));
    }

    @Test
    void secondsAreMentionedOnlyWhenNotZero() {
        assertEquals(
                "At second 30, every 5 minutes",
                CronDescriber.describe(explain.execute(new CronRequest("30 */5 * * * *", CronTarget.GO, true, JAKARTA))
                        .meaning()));
        assertEquals("Every 10 seconds", describe("*/10 * * * * *", CronTarget.SPRING));
        assertEquals("Every day at 08:30:15", describe("15 30 8 * * *", CronTarget.SPRING));
    }

    @Test
    void goInterval() {
        assertEquals("Every 1 hour 30 minutes", describe("@every 1h30m", CronTarget.GO));
        assertEquals("1 day 2 hours 5 seconds", CronDescriber.duration(Duration.ofSeconds(93_605)));
    }

    @Test
    void fieldMeaningForBreakdownTable() {
        assertEquals(
                "Every 15 minutes",
                CronDescriber.field(CronFieldKind.MINUTE, new FieldExpr.Step(new FieldExpr.Any(), 15)));
        assertEquals("Hour 08 through 17", CronDescriber.field(CronFieldKind.HOUR, new FieldExpr.Range(8, 17)));
        assertEquals("Every month", CronDescriber.field(CronFieldKind.MONTH, new FieldExpr.Any()));
        assertEquals(
                "Every 5 minutes starting minute 3",
                CronDescriber.field(CronFieldKind.MINUTE, new FieldExpr.Step(new FieldExpr.Value(3), 5)));
        assertEquals("In 2027", CronDescriber.field(CronFieldKind.YEAR, new FieldExpr.Value(2027)));
        assertEquals("Other construct: ??", CronDescriber.field(CronFieldKind.MINUTE, new FieldExpr.Raw("??")));
    }
}
