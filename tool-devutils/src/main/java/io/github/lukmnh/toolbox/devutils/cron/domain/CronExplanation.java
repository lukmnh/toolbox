package io.github.lukmnh.toolbox.devutils.cron.domain;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

/**
 * @param scheduleZone zona tempat jadwal dievaluasi
 * @param nextRuns jadwal berikutnya, sudah dikonversi ke zona tampilan
 * @param snippet contoh pemakaian di kode target
 */
public record CronExplanation(
        CronTarget target, CronMeaning meaning, ZoneId scheduleZone, List<ZonedDateTime> nextRuns, String snippet) {

    public CronExplanation {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(meaning, "meaning");
        Objects.requireNonNull(scheduleZone, "scheduleZone");
        nextRuns = List.copyOf(nextRuns);
        Objects.requireNonNull(snippet, "snippet");
    }
}
