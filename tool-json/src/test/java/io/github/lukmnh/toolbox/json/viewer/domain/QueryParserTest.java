package io.github.lukmnh.toolbox.json.viewer.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.json.shared.domain.InvalidQueryException;
import io.github.lukmnh.toolbox.json.shared.domain.InvalidQueryException.Reason;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class QueryParserTest {

    /** {"status":"active","age":31,"role":"admin","address":{"city":"Bandung"},"items":[{"price":150000}],
     *  "email":"budi@contoh.id","deletedAt":null,"name":"Budi Santoso","vip":true} */
    private static final JsonValue RECORD = object(
            "status", str("active"),
            "age", num("31"),
            "role", str("admin"),
            "address", object("city", str("Bandung")),
            "items", new JsonValue.JsonArray(List.of(object("price", num("150000")))),
            "email", str("budi@contoh.id"),
            "deletedAt", JsonValue.NULL,
            "name", str("Budi Santoso"),
            "vip", new JsonValue.JsonBoolean(true));

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "status = \"active\" AND age >= 30 | true",
                "(role = \"admin\" OR role = \"owner\") AND address.city = \"Bandung\" | true",
                "items[0].price > 100000 AND email exists | true",
                "age = 31.0 | true",
                "age != 31 | false",
                "age < 30 OR vip = true | true",
                "name contains \"budi\" | true",
                "name CONTAINS \"SANTO\" | true",
                "email startsWith \"BUDI@\" | true",
                "deletedAt isNull | true",
                "deletedAt exists | true",
                "deletedAt = null | true",
                "missing exists | false",
                "missing isNull | false",
                "missing = \"x\" | false",
                "missing != \"x\" | true",
                "age = \"31\" | false",
                "status > \"abc\" AND status < \"b\" | true",
                "items[5].price > 0 | false",
                "a = 1 OR b = 2 AND c = 3 | false",
                "status = \"active\" or age = 1 and vip = false | true",
            })
    void evaluatesQueriesAgainstRecord(String query, boolean expected) {
        assertEquals(expected, QueryParser.parse(query).matches(RECORD), query);
    }

    @Test
    void andBindsTighterThanOr() {
        Query query = QueryParser.parse("a exists OR b exists AND c exists");

        Query.Or or = assertInstanceOf(Query.Or.class, query);
        assertInstanceOf(Query.And.class, or.right());
    }

    @Test
    void stringEscapesAreSupported() {
        JsonValue record = object("q", str("say \"hi\"\\now"));

        assertTrue(QueryParser.parse("q = \"say \\\"hi\\\"\\\\now\"").matches(record));
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "'' | EMPTY | 0",
                "status | EXPECTED_OPERATOR | 6",
                "status = | EXPECTED_VALUE | 8",
                "status = active | EXPECTED_VALUE | 9",
                "status = \"active | UNCLOSED_STRING | 9",
                "(status = \"a\" | UNCLOSED_PARENTHESIS | 0",
                "status = \"a\" AND | EXPECTED_FIELD | 16",
                "status = \"a\" )| UNEXPECTED_TOKEN | 13",
                "a ! 1 | UNEXPECTED_TOKEN | 2",
                "a..b = 1 | INVALID_PATH | 2",
                "items[x] = 1 | INVALID_PATH | 5",
                "= 1 | EXPECTED_FIELD | 0",
            })
    void invalidQueriesPointToProblemPosition(String query, Reason reason, int position) {
        InvalidQueryException error = assertThrows(InvalidQueryException.class, () -> QueryParser.parse(query));

        assertEquals(reason, error.reason(), query);
        assertEquals(position, error.position(), query);
    }

    @Test
    void fieldPathResolvesNestedKeysAndIndexes() {
        FieldPath path = FieldPath.parse("items[0].price", 0);

        assertEquals(num("150000"), path.resolve(RECORD).orElseThrow());
        assertFalse(FieldPath.parse("address.city.zip", 0).resolve(RECORD).isPresent());
    }

    static JsonValue object(Object... keyValues) {
        Map<String, JsonValue> fields = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            fields.put((String) keyValues[i], (JsonValue) keyValues[i + 1]);
        }
        return new JsonValue.JsonObject(fields);
    }

    static JsonValue str(String value) {
        return new JsonValue.JsonString(value);
    }

    static JsonValue num(String text) {
        return new JsonValue.JsonNumber(text);
    }
}
