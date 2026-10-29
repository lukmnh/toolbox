package io.github.lukmnh.toolbox.devutils.cron.domain;

import java.time.ZoneId;
import java.util.Objects;

/**
 * @param goWithSeconds Go: scheduler dibuat dengan {@code cron.WithSeconds()} (6 field)
 * @param zone zona waktu jadwal dan tampilan
 */
public record CronRequest(String expression, CronTarget target, boolean goWithSeconds, ZoneId zone) {

    public CronRequest {
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(zone, "zone");
    }
}
