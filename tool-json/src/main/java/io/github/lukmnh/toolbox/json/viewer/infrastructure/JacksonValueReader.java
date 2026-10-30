package io.github.lukmnh.toolbox.json.viewer.infrastructure;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonValue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Membangun {@link JsonValue} dari token Jackson. Hanya untuk nilai kecil (satu record, file recent). */
final class JacksonValueReader {

    private static final JsonValue TRUE = new JsonValue.JsonBoolean(true);
    private static final JsonValue FALSE = new JsonValue.JsonBoolean(false);

    private JacksonValueReader() {}

    /** Baca nilai mulai dari token saat ini; parser berakhir di token terakhir nilai tersebut. */
    static JsonValue read(JsonParser parser) throws IOException {
        JsonToken token = parser.currentToken();
        return switch (token) {
            case START_OBJECT -> readObject(parser);
            case START_ARRAY -> readArray(parser);
            case VALUE_STRING -> new JsonValue.JsonString(parser.getText());
            // Teks angka asli dipertahankan supaya presisi tidak hilang
            case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> new JsonValue.JsonNumber(parser.getText());
            case VALUE_TRUE -> TRUE;
            case VALUE_FALSE -> FALSE;
            case VALUE_NULL -> JsonValue.NULL;
            default -> throw new IllegalStateException("Unexpected token: " + token);
        };
    }

    private static JsonValue readObject(JsonParser parser) throws IOException {
        Map<String, JsonValue> fields = new LinkedHashMap<>();
        while (parser.nextToken() == JsonToken.FIELD_NAME) {
            String name = parser.currentName();
            parser.nextToken();
            fields.put(name, read(parser));
        }
        return new JsonValue.JsonObject(fields);
    }

    private static JsonValue readArray(JsonParser parser) throws IOException {
        List<JsonValue> items = new ArrayList<>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            items.add(read(parser));
        }
        return new JsonValue.JsonArray(items);
    }
}
