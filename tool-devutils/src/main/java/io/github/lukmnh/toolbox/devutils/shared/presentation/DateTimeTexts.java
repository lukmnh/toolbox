package io.github.lukmnh.toolbox.devutils.shared.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import java.time.format.DateTimeFormatter;

/** Format waktu yang mudah dibaca, contoh {@code Sen, 2 Nov 2026 08:00 WIB}. Dipakai JWT Debugger dan Cron. */
public final class DateTimeTexts {

    public static final DateTimeFormatter HUMAN =
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm z", Messages.LOCALE);

    /** Untuk jadwal yang punya field detik. */
    public static final DateTimeFormatter HUMAN_WITH_SECONDS =
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss z", Messages.LOCALE);

    private DateTimeTexts() {}
}
