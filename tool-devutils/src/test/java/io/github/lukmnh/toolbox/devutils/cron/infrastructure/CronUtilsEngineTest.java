package io.github.lukmnh.toolbox.devutils.cron.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.devutils.cron.application.ExplainCronUseCase;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronExplanation;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronFieldKind;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronFields;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronInputParser;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronRequest;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronSchedule;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronTarget;
import io.github.lukmnh.toolbox.devutils.cron.domain.FieldExpr;
import io.github.lukmnh.toolbox.devutils.cron.domain.InvalidCronException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CronUtilsEngineTest {

    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");
    /** Kamis, 29 Oktober 2026 10:00 WIB. */
    private static final Instant NOW =
            ZonedDateTime.of(2026, 10, 29, 10, 0, 0, 0, JAKARTA).toInstant();

    private final CronUtilsEngine engine = new CronUtilsEngine();
    private final ExplainCronUseCase explain = new ExplainCronUseCase(engine, Clock.fixed(NOW, ZoneId.of("UTC")));

    private static CronFields fields(String expression, CronTarget target) {
        return (CronFields) CronInputParser.parse(new CronRequest(expression, target, false, JAKARTA))
                .spec();
    }

    private CronSchedule schedule(String expression, CronTarget target) {
        return engine.parse(fields(expression, target));
    }

    private List<LocalDateTime> next(String expression, CronTarget target) {
        CronExplanation explanation = explain.execute(new CronRequest(expression, target, false, JAKARTA));
        return explanation.nextRuns().stream()
                .map(ZonedDateTime::toLocalDateTime)
                .toList();
    }

    private static LocalDateTime at(int month, int day, int hour, int minute) {
        return LocalDateTime.of(2026, month, day, hour, minute);
    }

    @Test
    void unixWeekdayWindowNextRunsInJakarta() {
        List<LocalDateTime> runs = next("*/15 8-17 * * 1-5", CronTarget.UNIX);
        assertEquals(ExplainCronUseCase.NEXT_RUN_COUNT, runs.size());
        assertEquals(at(10, 29, 10, 15), runs.get(0));
        assertEquals(at(10, 29, 12, 30), runs.get(9));
    }

    @Test
    void mapsFieldsToDomainModel() {
        CronSchedule schedule = schedule("*/15 8-17 1,15 * 1-5", CronTarget.UNIX);
        assertEquals(new FieldExpr.Step(new FieldExpr.Any(), 15), schedule.field(CronFieldKind.MINUTE));
        assertEquals(new FieldExpr.Range(8, 17), schedule.field(CronFieldKind.HOUR));
        assertEquals(
                new FieldExpr.ListOf(List.of(new FieldExpr.Value(1), new FieldExpr.Value(15))),
                schedule.field(CronFieldKind.DAY_OF_MONTH));
        assertEquals(new FieldExpr.Any(), schedule.field(CronFieldKind.SECOND));
    }

    @Test
    void dayOfWeekIsNormalizedToIsoForEveryTarget() {
        FieldExpr monToFri = new FieldExpr.Range(DayOfWeek.MONDAY.getValue(), DayOfWeek.FRIDAY.getValue());
        assertEquals(
                monToFri, schedule("0 0 12 ? * MON-FRI", CronTarget.QUARKUS).field(CronFieldKind.DAY_OF_WEEK));
        assertEquals(monToFri, schedule("0 0 12 ? * 2-6", CronTarget.QUARKUS).field(CronFieldKind.DAY_OF_WEEK));
        assertEquals(monToFri, schedule("0 12 * * 1-5", CronTarget.UNIX).field(CronFieldKind.DAY_OF_WEEK));
        FieldExpr sunday = new FieldExpr.Value(DayOfWeek.SUNDAY.getValue());
        assertEquals(sunday, schedule("0 0 * * 0", CronTarget.UNIX).field(CronFieldKind.DAY_OF_WEEK));
        assertEquals(sunday, schedule("0 0 * * 7", CronTarget.LARAVEL).field(CronFieldKind.DAY_OF_WEEK));
        assertEquals(sunday, schedule("0 0 * * SUN", CronTarget.GO).field(CronFieldKind.DAY_OF_WEEK));
        assertEquals(sunday, schedule("0 0 0 ? * 1", CronTarget.QUARKUS).field(CronFieldKind.DAY_OF_WEEK));
    }

    @Test
    void sameDigitMeansDifferentDayPerTarget() {
        // "1" = Senin di Unix, tapi Minggu di Quartz
        assertEquals(
                DayOfWeek.MONDAY, next("0 9 * * 1", CronTarget.UNIX).getFirst().getDayOfWeek());
        assertEquals(
                DayOfWeek.SUNDAY,
                next("0 0 9 ? * 1", CronTarget.QUARKUS).getFirst().getDayOfWeek());
    }

    @Test
    void specialCharactersMapToDomain() {
        assertEquals(
                new FieldExpr.LastDayOfMonth(0),
                schedule("0 0 12 L * ?", CronTarget.QUARKUS).field(CronFieldKind.DAY_OF_MONTH));
        assertEquals(
                new FieldExpr.LastDayOfMonth(3),
                schedule("0 0 12 L-3 * *", CronTarget.SPRING).field(CronFieldKind.DAY_OF_MONTH));
        assertEquals(
                new FieldExpr.LastWeekdayOfMonth(),
                schedule("0 0 12 LW * ?", CronTarget.QUARKUS).field(CronFieldKind.DAY_OF_MONTH));
        assertEquals(
                new FieldExpr.NearestWeekday(15),
                schedule("0 12 15W * *", CronTarget.LARAVEL).field(CronFieldKind.DAY_OF_MONTH));
        assertEquals(
                new FieldExpr.LastDayOfWeek(DayOfWeek.FRIDAY.getValue()),
                schedule("0 0 12 ? * 6L", CronTarget.QUARKUS).field(CronFieldKind.DAY_OF_WEEK));
        assertEquals(
                new FieldExpr.NthDayOfWeek(DayOfWeek.FRIDAY.getValue(), 2),
                schedule("0 12 * * 5#2", CronTarget.LARAVEL).field(CronFieldKind.DAY_OF_WEEK));
    }

    @Test
    void lastDayAndNthWeekdayRuns() {
        assertEquals(at(10, 31, 12, 0), next("0 0 12 L * *", CronTarget.SPRING).getFirst());
        assertEquals(at(11, 13, 12, 0), next("0 12 * * 5#2", CronTarget.LARAVEL).getFirst());
        assertEquals(
                at(10, 30, 12, 0), next("0 0 12 ? * 6L", CronTarget.QUARKUS).getFirst());
    }

    @Test
    void dayOfMonthAndDayOfWeekSemanticsFollowPlatform() {
        // cron klasik, robfig, Laravel: tanggal 1 ATAU Senin
        List<LocalDateTime> either = List.of(at(11, 1, 0, 0), at(11, 2, 0, 0));
        assertEquals(either, next("0 0 1 * 1", CronTarget.UNIX).subList(0, 2));
        assertEquals(either, next("0 0 1 * 1", CronTarget.GO).subList(0, 2));
        assertEquals(either, next("0 0 1 * 1", CronTarget.LARAVEL).subList(0, 2));
        // Spring: tanggal 1 yang jatuh pada Senin
        assertEquals(
                LocalDateTime.of(2027, 2, 1, 0, 0),
                next("0 0 0 1 * MON", CronTarget.SPRING).getFirst());
    }

    @Test
    void impossibleDateHasNoRuns() {
        assertTrue(next("0 12 31 2 *", CronTarget.UNIX).isEmpty());
    }

    @Test
    void goTimeZonePrefixIsUsedForScheduleButShownInRequestZone() {
        CronExplanation explanation =
                explain.execute(new CronRequest("CRON_TZ=UTC 0 9 * * *", CronTarget.GO, false, JAKARTA));
        assertEquals(ZoneId.of("UTC"), explanation.scheduleZone());
        // 09:00 UTC = 16:00 WIB
        assertEquals(at(10, 29, 16, 0), explanation.nextRuns().getFirst().toLocalDateTime());
        assertEquals(JAKARTA, explanation.nextRuns().getFirst().getZone());
        assertTrue(explanation.snippet().contains("CRON_TZ=UTC 0 9 * * *"), explanation.snippet());
    }

    @Test
    void invalidFieldIsLocatedWithTargetRange() {
        InvalidCronException minute =
                assertThrows(InvalidCronException.class, () -> schedule("61 0 * * *", CronTarget.UNIX));
        assertEquals(InvalidCronException.Reason.INVALID_FIELD, minute.reason());
        assertEquals(CronFieldKind.MINUTE, minute.field().orElseThrow());
        assertEquals("61", minute.token());
        assertEquals(0, minute.min());
        assertEquals(59, minute.max());

        InvalidCronException goSunday =
                assertThrows(InvalidCronException.class, () -> schedule("0 0 * * 7", CronTarget.GO));
        assertEquals(CronFieldKind.DAY_OF_WEEK, goSunday.field().orElseThrow());
        assertEquals(6, goSunday.max());

        InvalidCronException quartzDay =
                assertThrows(InvalidCronException.class, () -> schedule("0 0 12 ? * 9", CronTarget.QUARKUS));
        assertEquals(CronFieldKind.DAY_OF_WEEK, quartzDay.field().orElseThrow());
        assertEquals(1, quartzDay.min());
        assertEquals(7, quartzDay.max());

        InvalidCronException hour =
                assertThrows(InvalidCronException.class, () -> schedule("0 0 25 * * ?", CronTarget.QUARKUS));
        assertEquals(CronFieldKind.HOUR, hour.field().orElseThrow());
    }

    @Test
    void quartzDayFieldsConflict() {
        InvalidCronException error =
                assertThrows(InvalidCronException.class, () -> schedule("0 0 12 1 * MON", CronTarget.QUARKUS));
        assertEquals(InvalidCronException.Reason.DAY_FIELDS_CONFLICT, error.reason());
    }

    @Test
    void goWithSecondsAndInterval() {
        CronExplanation seconds = explain.execute(new CronRequest("30 */5 * * * *", CronTarget.GO, true, JAKARTA));
        assertEquals(
                LocalDateTime.of(2026, 10, 29, 10, 0, 30),
                seconds.nextRuns().getFirst().toLocalDateTime());
        assertTrue(seconds.snippet().contains("cron.WithSeconds()"), seconds.snippet());

        CronExplanation every = explain.execute(new CronRequest("@every 1h30m", CronTarget.GO, false, JAKARTA));
        assertEquals(at(10, 29, 11, 30), every.nextRuns().getFirst().toLocalDateTime());
        assertInstanceOf(io.github.lukmnh.toolbox.devutils.cron.domain.GoInterval.class, every.meaning());
    }
}
