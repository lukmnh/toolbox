package io.github.lukmnh.toolbox.devutils.cron.domain;

import java.time.ZoneId;
import java.util.Objects;

/** @param scheduleZone zona jadwal: prefix {@code CRON_TZ=} (Go) bila ada, selain itu zona request */
public record ParsedCron(CronSpec spec, ZoneId scheduleZone) {

    public ParsedCron {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(scheduleZone, "scheduleZone");
    }
}
