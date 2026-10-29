package io.github.lukmnh.toolbox.devutils.cron.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Jadwal hasil parse; field yang tidak ada di target (misalnya detik di Unix) dianggap {@link FieldExpr.Any}. */
public record CronSchedule(CronTarget target, Map<CronFieldKind, FieldExpr> fields) implements CronMeaning {

    private static final FieldExpr ANY = new FieldExpr.Any();

    public CronSchedule {
        Objects.requireNonNull(target, "target");
        fields = fields.isEmpty() ? Map.of() : Map.copyOf(new EnumMap<>(fields));
    }

    public FieldExpr field(CronFieldKind kind) {
        return fields.getOrDefault(kind, ANY);
    }

    public boolean hasSeconds() {
        return fields.containsKey(CronFieldKind.SECOND);
    }
}
