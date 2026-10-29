package io.github.lukmnh.toolbox.devutils.cron.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lukmnh.toolbox.devutils.cron.domain.CronEngine;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronExplanation;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronFields;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronRequest;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronSchedule;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronTarget;
import io.github.lukmnh.toolbox.devutils.cron.domain.InvalidCronException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExplainCronUseCaseTest {

    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");
    private static final Instant NOW = Instant.parse("2026-10-29T03:00:00Z");

    /** Fake engine: jadwal kosong, jadwal berikutnya = setiap jam dari {@code from}. */
    private static final class HourlyEngine implements CronEngine {

        final List<CronFields> parsed = new ArrayList<>();
        ZonedDateTime lastFrom;

        @Override
        public CronSchedule parse(CronFields fields) {
            parsed.add(fields);
            return new CronSchedule(fields.target(), Map.of());
        }

        @Override
        public List<ZonedDateTime> nextExecutions(CronFields fields, ZonedDateTime from, int count) {
            lastFrom = from;
            List<ZonedDateTime> runs = new ArrayList<>();
            for (int hour = 1; hour <= count; hour++) {
                runs.add(from.plusHours(hour));
            }
            return runs;
        }
    }

    private final HourlyEngine engine = new HourlyEngine();
    private final ExplainCronUseCase useCase = new ExplainCronUseCase(engine, Clock.fixed(NOW, ZoneId.of("UTC")));

    @Test
    void evaluatesInScheduleZoneAndReturnsTenRunsInRequestZone() {
        CronExplanation explanation = useCase.execute(new CronRequest("0 9 * * *", CronTarget.LARAVEL, false, JAKARTA));
        assertEquals(ZonedDateTime.ofInstant(NOW, JAKARTA), engine.lastFrom);
        assertEquals(ExplainCronUseCase.NEXT_RUN_COUNT, explanation.nextRuns().size());
        assertEquals(JAKARTA, explanation.nextRuns().getFirst().getZone());
        assertEquals(
                "Schedule::command('app:job')->cron('0 9 * * *')->timezone('Asia/Jakarta');", explanation.snippet());
    }

    @Test
    void goPrefixZoneIsScheduleZoneButRunsShownInRequestZone() {
        CronExplanation explanation =
                useCase.execute(new CronRequest("CRON_TZ=UTC 0 9 * * *", CronTarget.GO, false, JAKARTA));
        assertEquals(ZoneId.of("UTC"), engine.lastFrom.getZone());
        assertEquals(ZoneId.of("UTC"), explanation.scheduleZone());
        assertEquals(JAKARTA, explanation.nextRuns().getFirst().getZone());
    }

    @Test
    void invalidInputNeverReachesEngine() {
        assertThrows(
                InvalidCronException.class,
                () -> useCase.execute(new CronRequest("0 0 L * *", CronTarget.GO, false, JAKARTA)));
        assertEquals(List.of(), engine.parsed);
    }
}
