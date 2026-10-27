package io.github.lukmnh.toolbox.json.viewer.domain;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Nilai JSON immutable untuk satu record (record kecil; file besar tidak pernah diubah ke bentuk ini). */
public sealed interface JsonValue {

    JsonValue NULL = new JsonNull();

    /** @param fields urutan key dipertahankan sesuai file */
    record JsonObject(Map<String, JsonValue> fields) implements JsonValue {
        public JsonObject {
            fields = Collections.unmodifiableMap(Objects.requireNonNull(fields, "fields"));
        }
    }

    record JsonArray(List<JsonValue> items) implements JsonValue {
        public JsonArray {
            items = Collections.unmodifiableList(Objects.requireNonNull(items, "items"));
        }
    }

    record JsonString(String value) implements JsonValue {
        public JsonString {
            Objects.requireNonNull(value, "value");
        }
    }

    /** @param text teks angka persis seperti di file, supaya presisi tidak hilang */
    record JsonNumber(String text) implements JsonValue {
        public JsonNumber {
            Objects.requireNonNull(text, "text");
        }

        public BigDecimal decimal() {
            return new BigDecimal(text);
        }
    }

    record JsonBoolean(boolean value) implements JsonValue {}

    record JsonNull() implements JsonValue {}

    default boolean isContainer() {
        return this instanceof JsonObject || this instanceof JsonArray;
    }

    /** Teks untuk tabel: string tanpa kutip, angka/boolean/null apa adanya, container sebagai JSON ringkas. */
    default String displayText(int maxChars) {
        return switch (this) {
            case JsonString string -> JsonWriter.truncate(string.value(), maxChars);
            case JsonNumber number -> number.text();
            case JsonBoolean bool -> String.valueOf(bool.value());
            case JsonNull ignored -> "null";
            case JsonObject ignored -> JsonWriter.preview(this, maxChars);
            case JsonArray ignored -> JsonWriter.preview(this, maxChars);
        };
    }
}
