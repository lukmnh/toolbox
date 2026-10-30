package io.github.lukmnh.toolbox.json.viewer.domain;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Format CSV RFC 4180: pemisah koma, baris CRLF, nilai nested ditulis sebagai string JSON (BR-JS-5). */
public final class CsvFormat {

    public static final String LINE_END = "\r\n";

    private static final char SEPARATOR = ',';
    private static final char QUOTE = '"';

    private CsvFormat() {}

    public static String row(List<String> fields) {
        return fields.stream().map(CsvFormat::escape).collect(Collectors.joining(String.valueOf(SEPARATOR)));
    }

    /** Field dikutip bila berisi koma, kutip, CR, atau LF; kutip di dalamnya digandakan. */
    public static String escape(String field) {
        boolean needsQuotes = field.indexOf(SEPARATOR) >= 0
                || field.indexOf(QUOTE) >= 0
                || field.indexOf('\r') >= 0
                || field.indexOf('\n') >= 0;
        if (!needsQuotes) {
            return field;
        }
        return QUOTE + field.replace("\"", "\"\"") + QUOTE;
    }

    /** Field tidak ada dan {@code null} menjadi sel kosong. */
    public static String cell(Optional<JsonValue> value) {
        if (value.isEmpty()) {
            return "";
        }
        return switch (value.get()) {
            case JsonValue.JsonNull ignored -> "";
            case JsonValue.JsonString string -> string.value();
            case JsonValue.JsonNumber number -> number.text();
            case JsonValue.JsonBoolean bool -> String.valueOf(bool.value());
            case JsonValue.JsonObject object -> JsonWriter.compact(object);
            case JsonValue.JsonArray array -> JsonWriter.compact(array);
        };
    }
}
