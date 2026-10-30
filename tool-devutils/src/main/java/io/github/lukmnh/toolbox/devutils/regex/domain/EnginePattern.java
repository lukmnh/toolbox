package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.List;
import java.util.Objects;

/**
 * Pola yang siap dikompilasi engine.
 *
 * @param source pola dalam sintaks engine (Java untuk {@link EngineKind#JDK}, Go/RE2 untuk {@link EngineKind#RE2})
 * @param flags flag {@code java.util.regex.Pattern} untuk JDK; 0 untuk RE2 (flag Go ditulis sebagai prefix inline)
 * @param groupNames nama tampilan grup per nomor (indeks 0 = seluruh match), nama asli dari pola walau engine
 *     memakai nama internal
 * @param firstMatchOnly JavaScript tanpa flag {@code g}: hanya match pertama
 */
public record EnginePattern(
        EngineKind engine,
        String source,
        int flags,
        List<String> groupNames,
        OffsetMap offsets,
        boolean firstMatchOnly) {

    public EnginePattern {
        Objects.requireNonNull(engine, "engine");
        Objects.requireNonNull(source, "source");
        groupNames = List.copyOf(groupNames);
        Objects.requireNonNull(offsets, "offsets");
    }

    public int groupCount() {
        return groupNames.size() - 1;
    }
}
