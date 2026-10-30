package io.github.lukmnh.toolbox.json.viewer.domain;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Optional;

/**
 * Kunci urut satu nilai kolom. Urutan antar tipe: angka (numerik) → string (leksikal) → boolean → object/array.
 * Field yang tidak ada atau {@code null} selalu di akhir, baik ascending maupun descending.
 */
public record SortKey(int rank, BigDecimal number, String text) {

    private static final int NUMBER = 0;
    private static final int STRING = 1;
    private static final int BOOLEAN = 2;
    private static final int CONTAINER = 3;
    private static final int MISSING = 4;
    /** Container hanya diurutkan berdasarkan awal JSON-nya supaya memori tetap kecil. */
    private static final int CONTAINER_TEXT_LIMIT = 200;

    private static final SortKey MISSING_KEY = new SortKey(MISSING, null, "");

    private static final Comparator<SortKey> NATURAL = Comparator.comparingInt(SortKey::rank)
            .thenComparing(SortKey::number, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(SortKey::text);

    public static SortKey of(Optional<JsonValue> value) {
        if (value.isEmpty()) {
            return MISSING_KEY;
        }
        return switch (value.get()) {
            case JsonValue.JsonNumber n -> new SortKey(NUMBER, n.decimal(), "");
            case JsonValue.JsonString s -> new SortKey(STRING, null, s.value());
            case JsonValue.JsonBoolean b -> new SortKey(BOOLEAN, null, String.valueOf(b.value()));
            case JsonValue.JsonNull ignored -> MISSING_KEY;
            case JsonValue.JsonObject o -> new SortKey(CONTAINER, null, JsonWriter.preview(o, CONTAINER_TEXT_LIMIT));
            case JsonValue.JsonArray a -> new SortKey(CONTAINER, null, JsonWriter.preview(a, CONTAINER_TEXT_LIMIT));
        };
    }

    public boolean isMissing() {
        return rank == MISSING;
    }

    /** Comparator dengan arah tertentu; nilai kosong tetap di akhir. */
    public static Comparator<SortKey> order(boolean ascending) {
        Comparator<SortKey> direction = ascending ? NATURAL : NATURAL.reversed();
        return (left, right) -> {
            if (left.isMissing() != right.isMissing()) {
                return left.isMissing() ? 1 : -1;
            }
            return direction.compare(left, right);
        };
    }
}
