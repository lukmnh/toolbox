package io.github.lukmnh.toolbox.common.ui;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import java.text.NumberFormat;

/** Format ukuran file yang mudah dibaca: {@code 512 B}, {@code 1,2 KB}, {@code 3,4 MB}. */
public final class ByteSizeFormat {

    private static final long UNIT = 1024;
    private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB"};
    private static final int FRACTION_DIGITS = 1;

    private ByteSizeFormat() {}

    public static String format(long bytes) {
        if (bytes < UNIT) {
            return bytes + " " + UNITS[0];
        }
        double value = bytes;
        int unitIndex = 0;
        while (value >= UNIT && unitIndex < UNITS.length - 1) {
            value /= UNIT;
            unitIndex++;
        }
        NumberFormat number = NumberFormat.getNumberInstance(Messages.LOCALE);
        number.setMaximumFractionDigits(FRACTION_DIGITS);
        return number.format(value) + " " + UNITS[unitIndex];
    }
}
