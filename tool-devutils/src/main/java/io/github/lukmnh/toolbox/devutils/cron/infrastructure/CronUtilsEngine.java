package io.github.lukmnh.toolbox.devutils.cron.infrastructure;

import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.field.CronField;
import com.cronutils.model.field.CronFieldName;
import com.cronutils.model.field.constraint.FieldConstraints;
import com.cronutils.model.field.definition.DayOfWeekFieldDefinition;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronEngine;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronFieldKind;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronFields;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronSchedule;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronTarget;
import io.github.lukmnh.toolbox.devutils.cron.domain.FieldExpr;
import io.github.lukmnh.toolbox.devutils.cron.domain.InvalidCronException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Adapter cron-utils: parse + validasi per target, mapping ke model domain, dan jadwal berikutnya. */
public final class CronUtilsEngine implements CronEngine {

    private static final String ANY = "*";
    private static final String NO_SPECIFIC = "?";
    private static final int DEFAULT_MONDAY_VALUE = 1;
    private static final Map<CronFieldKind, CronFieldName> NAMES = Map.of(
            CronFieldKind.SECOND, CronFieldName.SECOND,
            CronFieldKind.MINUTE, CronFieldName.MINUTE,
            CronFieldKind.HOUR, CronFieldName.HOUR,
            CronFieldKind.DAY_OF_MONTH, CronFieldName.DAY_OF_MONTH,
            CronFieldKind.MONTH, CronFieldName.MONTH,
            CronFieldKind.DAY_OF_WEEK, CronFieldName.DAY_OF_WEEK,
            CronFieldKind.YEAR, CronFieldName.YEAR);

    @Override
    public CronSchedule parse(CronFields fields) {
        CronDefinition definition = CronDefinitions.of(fields);
        Cron cron = parse(fields, definition);
        int mondayValue = mondayValue(definition);
        Map<CronFieldKind, FieldExpr> mapped = new EnumMap<>(CronFieldKind.class);
        for (CronFieldKind kind : fields.layout()) {
            CronField field = cron.retrieve(NAMES.get(kind));
            mapped.put(
                    kind,
                    field == null
                            ? new FieldExpr.Any()
                            : new FieldExpressionMapper(kind, mondayValue).map(field.getExpression()));
        }
        return new CronSchedule(fields.target(), mapped);
    }

    @Override
    public List<ZonedDateTime> nextExecutions(CronFields fields, ZonedDateTime from, int count) {
        ExecutionTime executionTime = ExecutionTime.forCron(parse(fields, CronDefinitions.of(fields)));
        List<ZonedDateTime> runs = new ArrayList<>(count);
        ZonedDateTime cursor = from;
        while (runs.size() < count) {
            Optional<ZonedDateTime> next = executionTime.nextExecution(cursor);
            if (next.isEmpty()) {
                break;
            }
            cursor = next.get();
            runs.add(cursor);
        }
        return List.copyOf(runs);
    }

    private static Cron parse(CronFields fields, CronDefinition definition) {
        try {
            return new CronParser(definition).parse(fields.expression()).validate();
        } catch (RuntimeException e) {
            // Pesan cron-utils berbahasa Inggris dan tidak menyebut field; cari sendiri field yang salah
            throw locate(fields, definition);
        }
    }

    private static boolean parses(CronDefinition definition, String expression) {
        try {
            new CronParser(definition).parse(expression).validate();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Uji tiap field sendirian (field lain diisi nilai netral); field pertama yang gagal adalah penyebabnya. */
    private static InvalidCronException locate(CronFields fields, CronDefinition definition) {
        for (int index = 0; index < fields.values().size(); index++) {
            if (!parses(definition, isolate(fields, index))) {
                CronFieldKind kind = fields.layout().get(index);
                FieldConstraints constraints =
                        definition.getFieldDefinition(NAMES.get(kind)).getConstraints();
                return InvalidCronException.invalidField(
                        kind, fields.values().get(index), constraints.getStartRange(), constraints.getEndRange());
            }
        }
        // Semua field valid sendiri-sendiri: masalahnya kombinasi, di Quartz pasti tanggal vs hari-minggu
        return fields.target() == CronTarget.QUARKUS
                ? InvalidCronException.dayFieldsConflict()
                : InvalidCronException.invalidExpression();
    }

    private static String isolate(CronFields fields, int keep) {
        CronFieldKind keptKind = fields.layout().get(keep);
        String kept = fields.values().get(keep);
        List<String> values = new ArrayList<>(fields.values().size());
        for (int index = 0; index < fields.values().size(); index++) {
            values.add(
                    index == keep
                            ? kept
                            : neutral(fields.target(), fields.layout().get(index), keptKind, kept));
        }
        return String.join(" ", values);
    }

    /** Quartz mewajibkan tepat satu {@code ?} di antara tanggal dan hari-minggu. */
    private static String neutral(CronTarget target, CronFieldKind kind, CronFieldKind keptKind, String kept) {
        if (target != CronTarget.QUARKUS) {
            return ANY;
        }
        boolean keptIsQuestionMark = kept.equals(NO_SPECIFIC);
        return switch (kind) {
            case DAY_OF_MONTH -> keptKind == CronFieldKind.DAY_OF_WEEK && !keptIsQuestionMark ? NO_SPECIFIC : ANY;
            case DAY_OF_WEEK -> keptKind == CronFieldKind.DAY_OF_MONTH && keptIsQuestionMark ? ANY : NO_SPECIFIC;
            default -> ANY;
        };
    }

    private static int mondayValue(CronDefinition definition) {
        return definition.getFieldDefinition(CronFieldName.DAY_OF_WEEK) instanceof DayOfWeekFieldDefinition dayOfWeek
                ? dayOfWeek.getMondayDoWValue().getMondayDoWValue()
                : DEFAULT_MONDAY_VALUE;
    }
}
