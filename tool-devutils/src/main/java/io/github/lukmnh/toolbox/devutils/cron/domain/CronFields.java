package io.github.lukmnh.toolbox.devutils.cron.domain;

import java.util.List;
import java.util.Objects;

/**
 * Field ekspresi yang sudah lolos cek jumlah field dan karakter khusus.
 *
 * @param layout jenis tiap field, urut seperti {@code values}
 */
public record CronFields(CronTarget target, List<CronFieldKind> layout, List<String> values) implements CronSpec {

    public CronFields {
        Objects.requireNonNull(target, "target");
        layout = List.copyOf(layout);
        values = List.copyOf(values);
        if (layout.size() != values.size()) {
            throw new IllegalArgumentException("Field count does not match the layout");
        }
    }

    /** Ekspresi siap dipakai library / ditulis ke kode. */
    public String expression() {
        return String.join(" ", values);
    }

    public boolean hasSeconds() {
        return layout.contains(CronFieldKind.SECOND);
    }
}
