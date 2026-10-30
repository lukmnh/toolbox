package io.github.lukmnh.toolbox.devutils.cron.infrastructure;

import com.cronutils.model.field.expression.Always;
import com.cronutils.model.field.expression.And;
import com.cronutils.model.field.expression.Between;
import com.cronutils.model.field.expression.Every;
import com.cronutils.model.field.expression.FieldExpression;
import com.cronutils.model.field.expression.On;
import com.cronutils.model.field.expression.QuestionMark;
import com.cronutils.model.field.value.SpecialChar;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronFieldKind;
import io.github.lukmnh.toolbox.devutils.cron.domain.FieldExpr;
import java.time.DayOfWeek;

/** Model cron-utils → {@link FieldExpr}; hari-minggu dinormalisasi ke ISO (1 = Senin). */
final class FieldExpressionMapper {

    private static final int DAYS_PER_WEEK = 7;
    /** cron-utils memakai -1 untuk "tidak diisi" pada {@code On.getTime()} / {@code getNth()}. */
    private static final int UNSET = -1;

    private final CronFieldKind kind;
    private final int mondayValue;

    /** @param mondayValue nilai Senin di penomoran target (Unix 1, Quartz 2) */
    FieldExpressionMapper(CronFieldKind kind, int mondayValue) {
        this.kind = kind;
        this.mondayValue = mondayValue;
    }

    FieldExpr map(FieldExpression expression) {
        return switch (expression) {
            case Always a -> new FieldExpr.Any();
            case QuestionMark q -> new FieldExpr.Any();
            case On on -> on(on);
            case Between between -> range(between);
            case Every every ->
                new FieldExpr.Step(map(every.getExpression()), every.getPeriod().getValue());
            case And and ->
                new FieldExpr.ListOf(
                        and.getExpressions().stream().map(this::map).toList());
            default -> new FieldExpr.Raw(expression.asString());
        };
    }

    private FieldExpr on(On on) {
        int time = on.getTime().getValue();
        int nth = on.getNth().getValue();
        SpecialChar special = on.getSpecialChar().getValue();
        return switch (special) {
            case NONE -> new FieldExpr.Value(value(time));
            case L ->
                kind == CronFieldKind.DAY_OF_WEEK
                        ? lastDayOfWeek(time)
                        : new FieldExpr.LastDayOfMonth(nth == UNSET ? 0 : nth);
            case LW -> new FieldExpr.LastWeekdayOfMonth();
            case W -> new FieldExpr.NearestWeekday(time);
            case HASH -> new FieldExpr.NthDayOfWeek(isoDay(time), nth);
            case QUESTION_MARK -> new FieldExpr.Any();
        };
    }

    /** Quartz: {@code L} sendirian di hari-minggu berarti hari ke-7 (Sabtu), bukan "terakhir". */
    private FieldExpr lastDayOfWeek(int time) {
        return time == UNSET
                ? new FieldExpr.Value(DayOfWeek.SATURDAY.getValue())
                : new FieldExpr.LastDayOfWeek(isoDay(time));
    }

    private FieldExpr range(Between between) {
        if (!(between.getFrom().getValue() instanceof Integer from)
                || !(between.getTo().getValue() instanceof Integer to)) {
            return new FieldExpr.Raw(between.asString());
        }
        return new FieldExpr.Range(value(from), value(to));
    }

    private int value(int raw) {
        return kind == CronFieldKind.DAY_OF_WEEK ? isoDay(raw) : raw;
    }

    private int isoDay(int raw) {
        return Math.floorMod(raw - mondayValue, DAYS_PER_WEEK) + 1;
    }
}
