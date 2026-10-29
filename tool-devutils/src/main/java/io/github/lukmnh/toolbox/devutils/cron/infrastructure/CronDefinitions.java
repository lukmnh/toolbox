package io.github.lukmnh.toolbox.devutils.cron.infrastructure;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronFields;

/**
 * Definisi cron-utils per target. Go dan Laravel tidak ada bawaannya, jadi dibuat sendiri mengikuti parser aslinya.
 *
 * <p>Definisi custom sengaja tanpa {@code ?}: input sudah dinormalisasi {@code ?} → {@code *}, dan cron-utils hanya
 * memakai semantik OR untuk tanggal + hari-minggu (seperti robfig & dragonmantank) bila {@code ?} tidak didukung.
 */
final class CronDefinitions {

    private static final int MAX_SECOND = 59;
    private static final int MAX_MINUTE = 59;
    private static final int MAX_HOUR = 23;
    private static final int MAX_DAY_OF_MONTH = 31;
    private static final int MAX_MONTH = 12;
    private static final int MONDAY = 1;
    private static final int SATURDAY = 6;
    private static final int SUNDAY_ALIAS = 7;
    private static final int SUNDAY = 0;

    private static final CronDefinition UNIX = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);
    private static final CronDefinition SPRING = CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING53);
    private static final CronDefinition QUARTZ = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
    private static final CronDefinition GO = go(false);
    private static final CronDefinition GO_WITH_SECONDS = go(true);
    private static final CronDefinition LARAVEL = laravel();

    private CronDefinitions() {}

    static CronDefinition of(CronFields fields) {
        return switch (fields.target()) {
            case UNIX -> UNIX;
            case SPRING -> SPRING;
            case QUARKUS -> QUARTZ;
            case GO -> fields.hasSeconds() ? GO_WITH_SECONDS : GO;
            case LARAVEL -> LARAVEL;
        };
    }

    /** robfig/cron v3 parser standar: hari-minggu 0–6, tanpa L/W/#. */
    private static CronDefinition go(boolean withSeconds) {
        CronDefinitionBuilder builder = CronDefinitionBuilder.defineCron();
        if (withSeconds) {
            builder = builder.withSeconds()
                    .withValidRange(0, MAX_SECOND)
                    .withStrictRange()
                    .and();
        }
        return builder.withMinutes()
                .withValidRange(0, MAX_MINUTE)
                .withStrictRange()
                .and()
                .withHours()
                .withValidRange(0, MAX_HOUR)
                .withStrictRange()
                .and()
                .withDayOfMonth()
                .withValidRange(1, MAX_DAY_OF_MONTH)
                .withStrictRange()
                .and()
                .withMonth()
                .withValidRange(1, MAX_MONTH)
                .withStrictRange()
                .and()
                .withDayOfWeek()
                .withValidRange(SUNDAY, SATURDAY)
                .withMondayDoWValue(MONDAY)
                .withStrictRange()
                .and()
                .instance();
    }

    /** dragonmantank/cron-expression: hari-minggu 0–7 (7 = Minggu), {@code L} & {@code 15W} di tanggal, {@code 5L} & {@code 5#2}. */
    private static CronDefinition laravel() {
        return CronDefinitionBuilder.defineCron()
                .withMinutes()
                .withValidRange(0, MAX_MINUTE)
                .withStrictRange()
                .and()
                .withHours()
                .withValidRange(0, MAX_HOUR)
                .withStrictRange()
                .and()
                .withDayOfMonth()
                .withValidRange(1, MAX_DAY_OF_MONTH)
                .supportsL()
                .supportsW()
                .withStrictRange()
                .and()
                .withMonth()
                .withValidRange(1, MAX_MONTH)
                .withStrictRange()
                .and()
                .withDayOfWeek()
                .withValidRange(SUNDAY, SUNDAY_ALIAS)
                .withMondayDoWValue(MONDAY)
                .withIntMapping(SUNDAY_ALIAS, SUNDAY)
                .supportsL()
                .supportsHash()
                .withStrictRange()
                .and()
                .instance();
    }
}
