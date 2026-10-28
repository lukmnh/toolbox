package io.github.lukmnh.toolbox.devutils.jwt.infrastructure;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import io.github.lukmnh.toolbox.devutils.jwt.domain.Claim;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JwtJson;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JwtPart;
import io.github.lukmnh.toolbox.devutils.jwt.domain.MalformedJwtException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** {@link JwtJson} dengan Jackson streaming (tanpa databind). */
public final class JacksonJwtJson implements JwtJson {

    private static final String ALGORITHM_FIELD = "alg";
    private static final DefaultIndenter INDENTER = new DefaultIndenter("  ", "\n");

    @Override
    public String prettyPrint(String json, JwtPart part) {
        try (JsonParser parser = JwtJsonFactory.FACTORY.createParser(json)) {
            requireObject(parser, part);
            StringWriter writer = new StringWriter();
            try (JsonGenerator generator = JwtJsonFactory.FACTORY.createGenerator(writer)) {
                generator.setPrettyPrinter(prettyPrinter());
                generator.copyCurrentStructure(parser);
            }
            requireEnd(parser, part);
            return writer.toString();
        } catch (JsonProcessingException e) {
            throw invalidJson(part, e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public List<Claim> claims(String payloadJson) {
        try (JsonParser parser = JwtJsonFactory.FACTORY.createParser(payloadJson)) {
            requireObject(parser, JwtPart.PAYLOAD);
            List<Claim> claims = new ArrayList<>();
            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                String name = parser.currentName();
                JsonToken value = parser.nextToken();
                claims.add(new Claim(name, valueText(parser, value), typeOf(value)));
            }
            return claims;
        } catch (JsonProcessingException e) {
            throw invalidJson(JwtPart.PAYLOAD, e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<String> algorithm(String headerJson) {
        try (JsonParser parser = JwtJsonFactory.FACTORY.createParser(headerJson)) {
            requireObject(parser, JwtPart.HEADER);
            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                String name = parser.currentName();
                JsonToken value = parser.nextToken();
                if (ALGORITHM_FIELD.equals(name) && value == JsonToken.VALUE_STRING) {
                    return Optional.of(parser.getText());
                }
                parser.skipChildren();
            }
            return Optional.empty();
        } catch (JsonProcessingException e) {
            throw invalidJson(JwtPart.HEADER, e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Format {@code "key": value} (tanpa spasi sebelum titik dua) dengan indent 2 spasi, termasuk array. */
    private static DefaultPrettyPrinter prettyPrinter() {
        Separators separators =
                Separators.createDefaultInstance().withObjectFieldValueSpacing(Separators.Spacing.AFTER);
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter().withSeparators(separators);
        printer.indentObjectsWith(INDENTER);
        printer.indentArraysWith(INDENTER);
        return printer;
    }

    private static void requireObject(JsonParser parser, JwtPart part) throws IOException {
        if (parser.nextToken() != JsonToken.START_OBJECT) {
            throw new MalformedJwtException(part, MalformedJwtException.Reason.NOT_JSON_OBJECT);
        }
    }

    /** Konten setelah object penutup ({@code {}x}) membuat JSON tidak valid. */
    private static void requireEnd(JsonParser parser, JwtPart part) throws IOException {
        if (parser.nextToken() != null) {
            throw new MalformedJwtException(part, MalformedJwtException.Reason.INVALID_JSON, position(parser));
        }
    }

    private static String valueText(JsonParser parser, JsonToken value) throws IOException {
        return switch (value) {
            case START_ARRAY, START_OBJECT -> compact(parser);
            default -> parser.getText();
        };
    }

    private static String compact(JsonParser parser) throws IOException {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = JwtJsonFactory.FACTORY.createGenerator(writer)) {
            generator.copyCurrentStructure(parser);
        }
        return writer.toString();
    }

    private static Claim.ValueType typeOf(JsonToken value) {
        return switch (value) {
            case VALUE_STRING -> Claim.ValueType.STRING;
            case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> Claim.ValueType.NUMBER;
            case VALUE_TRUE, VALUE_FALSE -> Claim.ValueType.BOOLEAN;
            case START_ARRAY -> Claim.ValueType.ARRAY;
            case START_OBJECT -> Claim.ValueType.OBJECT;
            default -> Claim.ValueType.NULL;
        };
    }

    /**
     * Hanya posisi (baris:kolom) yang diteruskan, bukan pesan Jackson, karena pesan library bisa memuat potongan isi
     * token (Security #5).
     */
    private static MalformedJwtException invalidJson(JwtPart part, JsonProcessingException error) {
        JsonLocation location = error.getLocation();
        String position = location == null ? "" : location.getLineNr() + ":" + location.getColumnNr();
        return new MalformedJwtException(part, MalformedJwtException.Reason.INVALID_JSON, position);
    }

    private static String position(JsonParser parser) {
        JsonLocation location = parser.currentLocation();
        return location.getLineNr() + ":" + location.getColumnNr();
    }
}
