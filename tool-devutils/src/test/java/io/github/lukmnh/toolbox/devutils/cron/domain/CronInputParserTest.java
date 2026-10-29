package io.github.lukmnh.toolbox.devutils.cron.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class CronInputParserTest {

    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");

    private static ParsedCron parse(String expression, CronTarget target) {
        return CronInputParser.parse(new CronRequest(expression, target, false, JAKARTA));
    }

    private static CronFields fields(String expression, CronTarget target) {
        return assertInstanceOf(CronFields.class, parse(expression, target).spec());
    }

    private static InvalidCronException invalid(String expression, CronTarget target) {
        return assertThrows(InvalidCronException.class, () -> parse(expression, target));
    }

    @Test
    void layoutFollowsTargetFieldCount() {
        assertEquals(
                CronFieldKind.FIVE, fields("*/15 8-17 * * 1-5", CronTarget.UNIX).layout());
        assertEquals(
                CronFieldKind.SIX,
                fields("0 */15 8-17 * * MON-FRI", CronTarget.SPRING).layout());
        assertEquals(
                CronFieldKind.SEVEN,
                fields("0 0 12 ? * MON 2027", CronTarget.QUARKUS).layout());
        CronFields goSeconds = assertInstanceOf(
                CronFields.class,
                CronInputParser.parse(new CronRequest("30 * * * * *", CronTarget.GO, true, JAKARTA))
                        .spec());
        assertEquals(CronFieldKind.SIX, goSeconds.layout());
    }

    @Test
    void wrongFieldCountReportsExpectedCounts() {
        InvalidCronException error = invalid("0 0 12 * *", CronTarget.QUARKUS);
        assertEquals(InvalidCronException.Reason.FIELD_COUNT, error.reason());
        assertEquals(List.of(6, 7), error.expectedCounts());
        assertEquals(5, error.actualCount());
        // Ekspresi Spring (6 field) ditempel di target Unix
        assertEquals(List.of(5), invalid("0 0 12 * * *", CronTarget.UNIX).expectedCounts());
    }

    @Test
    void questionMarkIsWildcardExceptQuartz() {
        assertEquals("0 12 * * 5", fields("0 12 ? * 5", CronTarget.LARAVEL).expression());
        assertEquals(
                "0 0 12 * * MON", fields("0 0 12 ? * MON", CronTarget.SPRING).expression());
        assertEquals(
                "0 0 12 ? * MON", fields("0 0 12 ? * MON", CronTarget.QUARKUS).expression());
        InvalidCronException unix = invalid("0 12 ? * 1", CronTarget.UNIX);
        assertEquals(InvalidCronException.Reason.UNSUPPORTED_SPECIAL, unix.reason());
        assertEquals(CronFieldKind.DAY_OF_MONTH, unix.field().orElseThrow());
    }

    @Test
    void specialCharactersAreCheckedPerTarget() {
        InvalidCronException go = invalid("0 0 L * *", CronTarget.GO);
        assertEquals(InvalidCronException.Reason.UNSUPPORTED_SPECIAL, go.reason());
        assertEquals("L", go.token());
        // Laravel mendukung L dan 15W, tapi tidak LW / L-3
        fields("0 0 L * *", CronTarget.LARAVEL);
        fields("0 0 15W * 5#2", CronTarget.LARAVEL);
        assertEquals(
                InvalidCronException.Reason.UNSUPPORTED_SPECIAL,
                invalid("0 0 LW * *", CronTarget.LARAVEL).reason());
        assertEquals("L-3", invalid("0 0 L-3 * *", CronTarget.LARAVEL).token());
        fields("0 0 12 L-3 * *", CronTarget.SPRING);
        // Nama hari WED bukan karakter W
        fields("0 9 * * WED", CronTarget.GO);
    }

    @Test
    void macrosExpandPerTarget() {
        assertEquals("0 0 * * *", fields("@daily", CronTarget.UNIX).expression());
        assertEquals("0 0 0 * * *", fields("@midnight", CronTarget.SPRING).expression());
        assertEquals("0 * * * *", fields("@HOURLY", CronTarget.GO).expression());
        assertEquals(
                InvalidCronException.Reason.UNSUPPORTED_MACRO,
                invalid("@daily", CronTarget.QUARKUS).reason());
        assertEquals(
                InvalidCronException.Reason.NO_SCHEDULE,
                invalid("@reboot", CronTarget.UNIX).reason());
        assertEquals(
                InvalidCronException.Reason.UNSUPPORTED_MACRO,
                invalid("@reboot", CronTarget.LARAVEL).reason());
        assertEquals("@sometimes", invalid("@sometimes", CronTarget.UNIX).token());
        assertEquals(
                InvalidCronException.Reason.INVALID_EXPRESSION,
                invalid("@daily 5", CronTarget.UNIX).reason());
    }

    @Test
    void goEveryIsInterval() {
        GoInterval interval = assertInstanceOf(
                GoInterval.class, parse("@every 1h30m", CronTarget.GO).spec());
        assertEquals(Duration.ofMinutes(90), interval.delay());
        assertEquals(
                InvalidCronException.Reason.UNSUPPORTED_MACRO,
                invalid("@every 1h", CronTarget.LARAVEL).reason());
    }

    @Test
    void goTimeZonePrefixOverridesZone() {
        ParsedCron parsed = parse("CRON_TZ=Europe/London 0 9 * * *", CronTarget.GO);
        assertEquals(ZoneId.of("Europe/London"), parsed.scheduleZone());
        assertEquals(
                "0 9 * * *", assertInstanceOf(CronFields.class, parsed.spec()).expression());
        assertEquals(ZoneId.of("UTC"), parse("TZ=UTC @daily", CronTarget.GO).scheduleZone());
        assertEquals(JAKARTA, parse("0 9 * * *", CronTarget.GO).scheduleZone());
        assertEquals(
                "Mars/Base",
                invalid("CRON_TZ=Mars/Base 0 9 * * *", CronTarget.GO).token());
    }

    @Test
    void blankInputIsEmpty() {
        assertEquals(
                InvalidCronException.Reason.EMPTY,
                invalid("   ", CronTarget.UNIX).reason());
        assertEquals(
                InvalidCronException.Reason.EMPTY,
                invalid("CRON_TZ=UTC", CronTarget.GO).reason());
    }
}
