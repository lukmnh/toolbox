package io.github.lukmnh.toolbox.json.viewer.domain;

import java.util.Objects;
import java.util.Optional;

/** Kolom di mode tabel. */
public sealed interface RecordColumn {

    /** Judul kolom di header CSV. */
    String header();

    /** Nilai kolom ini untuk satu record; kosong bila tidak ada. */
    Optional<JsonValue> extract(JsonValue record);

    /** Key top-level sebuah record object. */
    record Field(String key) implements RecordColumn {
        public Field {
            Objects.requireNonNull(key, "key");
        }

        @Override
        public String header() {
            return key;
        }

        @Override
        public Optional<JsonValue> extract(JsonValue record) {
            return record instanceof JsonValue.JsonObject object
                    ? Optional.ofNullable(object.fields().get(key))
                    : Optional.empty();
        }
    }

    /** Record yang bukan object (angka, string, array) ditampilkan utuh di kolom ini. */
    record WholeValue() implements RecordColumn {
        public static final String HEADER = "value";

        @Override
        public String header() {
            return HEADER;
        }

        @Override
        public Optional<JsonValue> extract(JsonValue record) {
            return record instanceof JsonValue.JsonObject ? Optional.empty() : Optional.of(record);
        }
    }

    /** Kolom {@code #} (nomor record, mulai 1). Tidak punya nilai JSON dan tidak ikut di-export. */
    record RecordNumber() implements RecordColumn {
        public static final String HEADER = "#";

        @Override
        public String header() {
            return HEADER;
        }

        @Override
        public Optional<JsonValue> extract(JsonValue record) {
            return Optional.empty();
        }
    }
}
