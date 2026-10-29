package io.github.lukmnh.toolbox.devutils.cron.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronFieldKind;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronTarget;
import io.github.lukmnh.toolbox.devutils.cron.domain.InvalidCronException;
import java.util.List;

/** Pesan error validasi yang menyebut field dan target, bukan pesan bahasa Inggris dari library. */
final class CronErrorMessages {

    private CronErrorMessages() {}

    static String describe(InvalidCronException error, CronTarget target) {
        String targetName = target.displayName();
        String field = error.field().map(CronErrorMessages::fieldName).orElse("");
        return switch (error.reason()) {
            case EMPTY -> Messages.get("devutils.cron.error.empty");
            case FIELD_COUNT ->
                Messages.format(
                        "devutils.cron.error.fieldCount",
                        targetName,
                        counts(error.expectedCounts()),
                        String.valueOf(error.actualCount()));
            case INVALID_FIELD ->
                Messages.format(
                        "devutils.cron.error.invalidField",
                        field,
                        error.token(),
                        targetName,
                        String.valueOf(error.min()),
                        String.valueOf(error.max()));
            case UNSUPPORTED_SPECIAL ->
                Messages.format("devutils.cron.error.unsupportedSpecial", field, error.token(), targetName);
            case UNSUPPORTED_MACRO ->
                Messages.format("devutils.cron.error.unsupportedMacro", targetName, error.token());
            case NO_SCHEDULE -> Messages.format("devutils.cron.error.noSchedule", error.token());
            case DAY_FIELDS_CONFLICT -> Messages.get("devutils.cron.error.dayFieldsConflict");
            case INVALID_INTERVAL -> Messages.format("devutils.cron.error.invalidInterval", error.token());
            case INVALID_TIME_ZONE -> Messages.format("devutils.cron.error.timeZone", error.token());
            case INVALID_EXPRESSION -> Messages.format("devutils.cron.error.invalid", targetName);
        };
    }

    static String fieldName(CronFieldKind kind) {
        return Messages.get("devutils.cron.field." + kind.name());
    }

    /** [6, 7] → "6 atau 7". */
    private static String counts(List<Integer> counts) {
        if (counts.size() == 1) {
            return String.valueOf(counts.getFirst());
        }
        return Messages.format(
                "devutils.cron.error.fieldCount.or",
                String.valueOf(counts.getFirst()),
                String.valueOf(counts.getLast()));
    }
}
