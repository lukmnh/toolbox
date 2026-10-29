package io.github.lukmnh.toolbox.devutils.cron.domain;

import java.time.ZonedDateTime;
import java.util.List;

/** Port ke library cron: validasi semantik per target dan perhitungan jadwal. */
public interface CronEngine {

    /**
     * @return model per field; nilai hari-minggu sudah ISO (1 = Senin)
     * @throws InvalidCronException dengan field yang salah bila ekspresi ditolak target
     */
    CronSchedule parse(CronFields fields);

    /**
     * @return paling banyak {@code count} jadwal setelah {@code from}; bisa lebih sedikit (bahkan kosong) bila
     *     ekspresi tidak pernah atau jarang terjadi, misalnya 31 Februari
     */
    List<ZonedDateTime> nextExecutions(CronFields fields, ZonedDateTime from, int count);
}
