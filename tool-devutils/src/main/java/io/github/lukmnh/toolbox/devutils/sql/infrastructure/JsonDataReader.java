package io.github.lukmnh.toolbox.devutils.sql.infrastructure;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import io.github.lukmnh.toolbox.devutils.shared.infrastructure.SafeJsonFactory;
import io.github.lukmnh.toolbox.devutils.sql.domain.DataTable;
import io.github.lukmnh.toolbox.devutils.sql.domain.DataTableBuilder;
import io.github.lukmnh.toolbox.devutils.sql.domain.DataValue;
import io.github.lukmnh.toolbox.devutils.sql.domain.InvalidDataException;
import io.github.lukmnh.toolbox.devutils.sql.domain.InvalidDataException.Problem;
import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Array of object (atau satu object) dibaca streaming. Kolom = gabungan key sesuai urutan pertama muncul. Tipe JSON
 * dipertahankan; object/array bersarang disimpan sebagai JSON compact.
 */
final class JsonDataReader {

    private JsonDataReader() {}

    static DataTable read(String fileName, String text, int maxRows) {
        DataTableBuilder builder = new DataTableBuilder(fileName, maxRows);
        try (JsonParser parser = SafeJsonFactory.FACTORY.createParser(text)) {
            JsonToken first = parser.nextToken();
            if (first == JsonToken.START_OBJECT) {
                builder.record(readObject(parser), line(parser));
            } else if (first == JsonToken.START_ARRAY) {
                for (JsonToken token = parser.nextToken(); token != JsonToken.END_ARRAY; token = parser.nextToken()) {
                    if (token != JsonToken.START_OBJECT) {
                        throw new InvalidDataException(fileName, Problem.NOT_OBJECTS, line(parser), "");
                    }
                    long line = line(parser);
                    builder.record(readObject(parser), line);
                }
            } else {
                throw new InvalidDataException(fileName, Problem.NOT_OBJECTS, line(parser), "");
            }
            if (parser.nextToken() != null) {
                throw new InvalidDataException(fileName, Problem.UNREADABLE, line(parser), "");
            }
        } catch (JsonProcessingException e) {
            JsonLocation location = e.getLocation();
            long line = location == null ? InvalidDataException.NO_LINE : location.getLineNr();
            throw new InvalidDataException(fileName, Problem.UNREADABLE, line, "", e);
        } catch (IOException e) {
            throw new InvalidDataException(fileName, Problem.UNREADABLE, InvalidDataException.NO_LINE, "", e);
        }
        return builder.build();
    }

    /** Parser berada di START_OBJECT; berhenti di END_OBJECT. */
    private static Map<String, DataValue> readObject(JsonParser parser) throws IOException {
        Map<String, DataValue> fields = new LinkedHashMap<>();
        while (parser.nextToken() == JsonToken.FIELD_NAME) {
            String name = parser.currentName();
            fields.put(name, value(parser, parser.nextToken()));
        }
        return fields;
    }

    private static DataValue value(JsonParser parser, JsonToken token) throws IOException {
        return switch (token) {
            case VALUE_STRING -> DataValue.text(parser.getText());
            case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> DataValue.number(parser.getText());
            case VALUE_TRUE -> DataValue.bool(true);
            case VALUE_FALSE -> DataValue.bool(false);
            case START_OBJECT, START_ARRAY -> DataValue.json(compact(parser));
            default -> DataValue.NULL;
        };
    }

    private static String compact(JsonParser parser) throws IOException {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = SafeJsonFactory.FACTORY.createGenerator(writer)) {
            generator.copyCurrentStructure(parser);
        }
        return writer.toString();
    }

    private static long line(JsonParser parser) {
        return parser.currentTokenLocation().getLineNr();
    }
}
