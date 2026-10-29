package io.github.lukmnh.toolbox.devutils.cron.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Macro {@code @daily} dkk. dan padanan 5 field-nya. */
public enum CronMacro {
    YEARLY("@yearly", "0 0 1 1 *"),
    ANNUALLY("@annually", "0 0 1 1 *"),
    MONTHLY("@monthly", "0 0 1 * *"),
    WEEKLY("@weekly", "0 0 * * 0"),
    DAILY("@daily", "0 0 * * *"),
    MIDNIGHT("@midnight", "0 0 * * *"),
    HOURLY("@hourly", "0 * * * *"),
    /** Sekali saat sistem menyala; tidak punya jadwal waktu. */
    REBOOT("@reboot", "");

    private final String text;
    private final String fiveFields;

    CronMacro(String text, String fiveFields) {
        this.text = text;
        this.fiveFields = fiveFields;
    }

    public String text() {
        return text;
    }

    /** Padanan menit–hari-minggu; kosong untuk {@link #REBOOT}. */
    String fiveFields() {
        return fiveFields;
    }

    static Optional<CronMacro> of(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(macro -> macro.text.equals(lower)).findFirst();
    }
}
