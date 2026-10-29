package io.github.lukmnh.toolbox.devutils.cron.domain;

import java.time.ZoneId;

/** Contoh kode siap tempel per target, lengkap dengan zona waktu. */
public final class CronSnippet {

    /** Placeholder perintah/job; user menggantinya sendiri. */
    private static final String UNIX_COMMAND = "/usr/local/bin/job.sh";

    private static final String LARAVEL_COMMAND = "app:job";

    private CronSnippet() {}

    /**
     * @param expression ekspresi siap pakai (field ternormalisasi, atau {@code @every ...})
     * @param withSeconds Go: scheduler perlu {@code cron.WithSeconds()}
     */
    public static String of(CronTarget target, String expression, ZoneId zone, boolean withSeconds) {
        String zoneId = zone.getId();
        return switch (target) {
            case UNIX -> expression + " " + UNIX_COMMAND;
            case SPRING -> "@Scheduled(cron = \"" + expression + "\", zone = \"" + zoneId + "\")";
            case QUARKUS -> "@Scheduled(cron = \"" + expression + "\", timeZone = \"" + zoneId + "\")";
            case GO -> go(expression, zoneId, withSeconds);
            case LARAVEL ->
                "Schedule::command('" + LARAVEL_COMMAND + "')->cron('" + expression + "')->timezone('" + zoneId + "');";
        };
    }

    /** {@code @every} tidak bergantung zona, jadi prefix {@code CRON_TZ=} hanya untuk jadwal per field. */
    private static String go(String expression, String zoneId, boolean withSeconds) {
        String scheduler = withSeconds ? "cron.New(cron.WithSeconds())" : "cron.New()";
        String spec = expression.startsWith("@every") ? expression : "CRON_TZ=" + zoneId + " " + expression;
        return "c := " + scheduler + "\n" + "c.AddFunc(\"" + spec + "\", job)";
    }
}
