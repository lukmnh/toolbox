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
                "*/15 8-17 * * 1-5      | UNIX    | Setiap 15 menit, dari jam 08:00 sampai 17:59, hari Senin sampai Jumat",
                "0 */15 8-17 * * MON-FRI| SPRING  | Setiap 15 menit, dari jam 08:00 sampai 17:59, hari Senin sampai Jumat",
                "0 0 1 * *              | UNIX    | Setiap tanggal 1 pukul 00:00",
                "0 0 12 ? * MON-FRI     | QUARKUS | Setiap hari Senin sampai Jumat pukul 12:00",
                "0 9 * * *              | GO      | Setiap hari pukul 09:00",
                "0 8,12,17 * * *        | LARAVEL | Setiap hari pukul 08:00, 12:00, dan 17:00",
                "0 0 12 L * *           | SPRING  | Setiap hari terakhir bulan pukul 12:00",
                "0 0 12 L-3 * ?         | QUARKUS | Setiap 3 hari sebelum hari terakhir bulan pukul 12:00",
                "0 0 9 LW * ?           | QUARKUS | Setiap hari kerja terakhir bulan pukul 09:00",
                "0 9 15W * *            | LARAVEL | Setiap hari kerja terdekat dari tanggal 15 pukul 09:00",
                "0 9 * * 5#2            | LARAVEL | Setiap hari Jumat ke-2 dalam bulan pukul 09:00",
                "0 0 12 ? * 6L          | QUARKUS | Setiap hari Jumat terakhir dalam bulan pukul 12:00",
                "0 0 1 * 1              | UNIX    | Setiap tanggal 1 atau hari Senin pukul 00:00",
                "0 0 0 1 * MON          | SPRING  | Setiap tanggal 1, hanya jika jatuh pada hari Senin pukul 00:00",
                "@hourly                | UNIX    | Setiap jam tepat (menit 00)",
                "30 * * * *             | UNIX    | Setiap jam pada menit ke-30",
                "* * * * *              | UNIX    | Setiap menit",
                "0 */2 * * *            | GO      | Setiap 2 jam tepat (menit 00)",
                "@yearly                | SPRING  | Setiap tanggal 1 pukul 00:00, hanya bulan Januari",
                "0 0/15 8-17 ? * MON-FRI 2027 | QUARKUS | Setiap 15 menit, dari jam 08:00 sampai 17:59, hari Senin sampai Jumat, hanya tahun 2027",
                "0 9 * 1-3 *            | UNIX    | Setiap hari pukul 09:00, bulan Januari sampai Maret",
                "15 9-17 * * *          | UNIX    | Setiap jam pada menit ke-15, dari jam 09:15 sampai 17:15",
                "0 0 */2 * *            | UNIX    | Setiap 2 hari pukul 00:00",
                "0 0 * * 0              | UNIX    | Setiap hari Minggu pukul 00:00",
            })
    void describesCommonPatterns(String expression, CronTarget target, String expected) {
        assertEquals(expected, describe(expression.strip(), target));
    }

    @Test
    void secondsAreMentionedOnlyWhenNotZero() {
        assertEquals(
                "Pada detik ke-30, setiap 5 menit",
                CronDescriber.describe(explain.execute(new CronRequest("30 */5 * * * *", CronTarget.GO, true, JAKARTA))
                        .meaning()));
        assertEquals("Setiap 10 detik", describe("*/10 * * * * *", CronTarget.SPRING));
        assertEquals("Setiap hari pukul 08:30:15", describe("15 30 8 * * *", CronTarget.SPRING));
    }

    @Test
    void goInterval() {
        assertEquals("Setiap 1 jam 30 menit", describe("@every 1h30m", CronTarget.GO));
        assertEquals("1 hari 2 jam 5 detik", CronDescriber.duration(Duration.ofSeconds(93_605)));
    }

    @Test
    void fieldMeaningForBreakdownTable() {
        assertEquals(
                "Setiap 15 menit",
                CronDescriber.field(CronFieldKind.MINUTE, new FieldExpr.Step(new FieldExpr.Any(), 15)));
        assertEquals("Jam 08 sampai 17", CronDescriber.field(CronFieldKind.HOUR, new FieldExpr.Range(8, 17)));
        assertEquals("Setiap bulan", CronDescriber.field(CronFieldKind.MONTH, new FieldExpr.Any()));
        assertEquals(
                "Setiap 5 menit mulai menit 3",
                CronDescriber.field(CronFieldKind.MINUTE, new FieldExpr.Step(new FieldExpr.Value(3), 5)));
        assertEquals("Tahun 2027", CronDescriber.field(CronFieldKind.YEAR, new FieldExpr.Value(2027)));
        assertEquals("Konstruk lain: ??", CronDescriber.field(CronFieldKind.MINUTE, new FieldExpr.Raw("??")));
    }
}
