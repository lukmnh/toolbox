package io.github.lukmnh.toolbox.devutils.cron.application;

import io.github.lukmnh.toolbox.devutils.cron.domain.CronEngine;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronExplanation;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronFields;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronInputParser;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronRequest;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronSchedule;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronSnippet;
import io.github.lukmnh.toolbox.devutils.cron.domain.GoInterval;
import io.github.lukmnh.toolbox.devutils.cron.domain.ParsedCron;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;

/** Validasi ekspresi untuk target terpilih, lalu hitung model jadwal, 10 jadwal berikutnya, dan snippet kode. */
public final class ExplainCronUseCase {

    public static final int NEXT_RUN_COUNT = 10;

    private final CronEngine engine;
    private final Clock clock;

    /** @param clock sumber "sekarang"; test memakai {@code Clock.fixed} */
    public ExplainCronUseCase(CronEngine engine, Clock clock) {
        this.engine = engine;
        this.clock = clock;
    }

    /** @throws io.github.lukmnh.toolbox.devutils.cron.domain.InvalidCronException bila tidak valid */
    public CronExplanation execute(CronRequest request) {
        ParsedCron parsed = CronInputParser.parse(request);
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(parsed.scheduleZone()));
        return switch (parsed.spec()) {
            case CronFields fields -> {
                CronSchedule schedule = engine.parse(fields);
                List<ZonedDateTime> runs = engine.nextExecutions(fields, now, NEXT_RUN_COUNT);
                String snippet = CronSnippet.of(
                        request.target(), fields.expression(), parsed.scheduleZone(), fields.hasSeconds());
                yield new CronExplanation(
                        request.target(), schedule, parsed.scheduleZone(), inZone(runs, request), snippet);
            }
            case GoInterval interval -> {
                String snippet =
                        CronSnippet.of(request.target(), "@every " + interval.text(), parsed.scheduleZone(), false);
                yield new CronExplanation(
                        request.target(),
                        interval,
                        parsed.scheduleZone(),
                        inZone(interval.nextRuns(now, NEXT_RUN_COUNT), request),
                        snippet);
            }
        };
    }

    /** Jadwal ditampilkan di zona pilihan user walau ekspresi Go membawa {@code CRON_TZ=} lain. */
    private static List<ZonedDateTime> inZone(List<ZonedDateTime> runs, CronRequest request) {
        return runs.stream().map(run -> run.withZoneSameInstant(request.zone())).toList();
    }
}
