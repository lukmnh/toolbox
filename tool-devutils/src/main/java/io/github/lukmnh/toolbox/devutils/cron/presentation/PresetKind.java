package io.github.lukmnh.toolbox.devutils.cron.presentation;

/** Pilihan di Builder; parameter tiap preset ada di {@link CronViewModel}. */
enum PresetKind {
    EVERY_MINUTES,
    HOURLY,
    DAILY,
    WEEKDAYS,
    WEEKLY,
    MONTHLY,
    CUSTOM;

    String labelKey() {
        return "devutils.cron.preset." + name();
    }
}
