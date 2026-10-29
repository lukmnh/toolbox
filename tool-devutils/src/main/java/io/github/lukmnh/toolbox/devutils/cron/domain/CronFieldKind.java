package io.github.lukmnh.toolbox.devutils.cron.domain;

import java.util.List;

/** Field ekspresi cron, urut seperti ditulis. */
public enum CronFieldKind {
    SECOND,
    MINUTE,
    HOUR,
    DAY_OF_MONTH,
    MONTH,
    DAY_OF_WEEK,
    YEAR;

    static final List<CronFieldKind> FIVE = List.of(MINUTE, HOUR, DAY_OF_MONTH, MONTH, DAY_OF_WEEK);
    static final List<CronFieldKind> SIX = List.of(SECOND, MINUTE, HOUR, DAY_OF_MONTH, MONTH, DAY_OF_WEEK);
    static final List<CronFieldKind> SEVEN = List.of(SECOND, MINUTE, HOUR, DAY_OF_MONTH, MONTH, DAY_OF_WEEK, YEAR);
}
