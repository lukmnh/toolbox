package io.github.lukmnh.toolbox.json.beautify.infrastructure;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import io.github.lukmnh.toolbox.json.beautify.domain.BeautifyAction;
import io.github.lukmnh.toolbox.json.beautify.domain.FormatOptions;
import io.github.lukmnh.toolbox.json.beautify.domain.FormatStats;
import io.github.lukmnh.toolbox.json.beautify.domain.Indent;
import io.github.lukmnh.toolbox.json.beautify.domain.JsonFormatter;
import io.github.lukmnh.toolbox.json.beautify.domain.JsonInput;
import io.github.lukmnh.toolbox.json.beautify.domain.TextPreview;
import io.github.lukmnh.toolbox.json.shared.domain.ErrorSnippet;
import io.github.lukmnh.toolbox.json.shared.domain.JsonIoException;
import io.github.lukmnh.toolbox.json.shared.domain.MalformedJsonException;
import io.github.lukmnh.toolbox.json.shared.domain.ScanProgress;
import io.github.lukmnh.toolbox.json.shared.domain.Utf8Text;
import io.github.lukmnh.toolbox.json.shared.infrastructure.JacksonErrors;
import io.github.lukmnh.toolbox.json.shared.infrastructure.JsonFactories;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Format JSON dengan Jackson streaming ({@code JsonParser} → {@code JsonGenerator}), sehingga file ratusan MB
 * tidak pernah dimuat utuh. Hanya mode sort keys yang membangun tree di memori.
 */
public final class JacksonJsonFormatter implements JsonFormatter {

    private static final int PROGRESS_INTERVAL_TOKENS = 1 << 14;
    private static final int OUTPUT_BUFFER_BYTES = 1 << 16;
    private static final String LINE_FEED = "\n";

    @Override
    public long fileSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new JsonIoException(file, e);
        }
    }

    @Override
    public FormatStats format(JsonInput input, FormatOptions options, Path output, ScanProgress progress) {
        try (JsonParser parser = openParser(input);
                OutputStream out = openOutput(output);
                JsonGenerator generator = createGenerator(out, options)) {
            if (parser.nextToken() == null) {
                throw JacksonErrors.empty(sourceName(input));
            }
            Counter counter = new Counter(input.sizeBytes(), progress);
            if (options.sortKeys()) {
                writeNode(generator, readNode(parser, counter));
            } else {
                copyStreaming(parser, generator, counter);
            }
            rejectTrailingContent(parser, input);
            if (options.action() == BeautifyAction.BEAUTIFY) {
                generator.writeRaw(LINE_FEED);
            }
            return counter.stats();
        } catch (StreamReadException e) {
            throw malformed(input, e);
        } catch (IOException e) {
            throw new JsonIoException(output != null ? output : inputPath(input), e);
        }
    }

    @Override
    public TextPreview readText(Path file, int maxBytes) {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] bytes = in.readNBytes(maxBytes + 1);
            return new TextPreview(Utf8Text.decodePrefix(bytes, maxBytes), bytes.length > maxBytes);
        } catch (IOException e) {
            throw new JsonIoException(file, e);
        }
    }

    private static JsonParser openParser(JsonInput input) throws IOException {
        return switch (input) {
            case JsonInput.Text text -> JsonFactories.FACTORY.createParser(text.content());
            case JsonInput.FileInput file -> JsonFactories.FACTORY.createParser(Files.newInputStream(file.file()));
        };
    }

    private static OutputStream openOutput(Path output) throws IOException {
        if (output == null) {
            return OutputStream.nullOutputStream();
        }
        return new BufferedOutputStream(Files.newOutputStream(output), OUTPUT_BUFFER_BYTES);
    }

    private static JsonGenerator createGenerator(OutputStream out, FormatOptions options) throws IOException {
        JsonGenerator generator = JsonFactories.FACTORY.createGenerator(out, JsonEncoding.UTF8);
        if (options.action() == BeautifyAction.BEAUTIFY) {
            generator.setPrettyPrinter(prettyPrinter(options.indent()));
        }
        return generator;
    }

    /** Gaya umum editor: {@code "key": value}, {@code []}/{@code {}} untuk kosong, newline LF di semua OS. */
    private static DefaultPrettyPrinter prettyPrinter(Indent indent) {
        Separators separators = Separators.createDefaultInstance()
                .withObjectFieldValueSpacing(Separators.Spacing.AFTER)
                .withObjectEmptySeparator("")
                .withArrayEmptySeparator("");
        DefaultIndenter indenter = new DefaultIndenter(indent.unit(), LINE_FEED);
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter(separators);
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);
        return printer;
    }

    /** Salin satu nilai root token demi token. */
    private static void copyStreaming(JsonParser parser, JsonGenerator generator, Counter counter) throws IOException {
        int depth = 0;
        do {
            JsonToken token = parser.currentToken();
            counter.count(parser, token);
            if (token.isNumeric()) {
                // Teks angka asli ditulis apa adanya: 1e400 tidak berubah jadi 1E+400, 2.50 tetap 2.50
                generator.writeNumber(parser.getText());
            } else {
                generator.copyCurrentEvent(parser);
            }
            if (token.isStructStart()) {
                depth++;
            } else if (token.isStructEnd()) {
                depth--;
            }
        } while (depth > 0 && parser.nextToken() != null);
    }

    private static Node readNode(JsonParser parser, Counter counter) throws IOException {
        JsonToken token = parser.currentToken();
        counter.count(parser, token);
        return switch (token) {
            case START_OBJECT -> readObject(parser, counter);
            case START_ARRAY -> readArray(parser, counter);
            default -> new ScalarNode(token, parser.getText());
        };
    }

    private static ObjectNode readObject(JsonParser parser, Counter counter) throws IOException {
        Map<String, Node> fields = new TreeMap<>();
        while (parser.nextToken() == JsonToken.FIELD_NAME) {
            counter.count(parser, JsonToken.FIELD_NAME);
            String name = parser.currentName();
            parser.nextToken();
            fields.put(name, readNode(parser, counter));
        }
        return new ObjectNode(fields);
    }

    private static ArrayNode readArray(JsonParser parser, Counter counter) throws IOException {
        List<Node> items = new ArrayList<>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            items.add(readNode(parser, counter));
        }
        return new ArrayNode(items);
    }

    private static void writeNode(JsonGenerator generator, Node node) throws IOException {
        switch (node) {
            case ObjectNode object -> {
                generator.writeStartObject();
                for (Map.Entry<String, Node> field : object.fields().entrySet()) {
                    generator.writeFieldName(field.getKey());
                    writeNode(generator, field.getValue());
                }
                generator.writeEndObject();
            }
            case ArrayNode array -> {
                generator.writeStartArray();
                for (Node item : array.items()) {
                    writeNode(generator, item);
                }
                generator.writeEndArray();
            }
            case ScalarNode scalar -> writeScalar(generator, scalar);
        }
    }

    private static void writeScalar(JsonGenerator generator, ScalarNode scalar) throws IOException {
        switch (scalar.token()) {
            case VALUE_STRING -> generator.writeString(scalar.text());
            // Teks angka asli ditulis apa adanya supaya presisi tidak berubah
            case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> generator.writeNumber(scalar.text());
            case VALUE_TRUE -> generator.writeBoolean(true);
            case VALUE_FALSE -> generator.writeBoolean(false);
            case VALUE_NULL -> generator.writeNull();
            default -> throw new IllegalStateException("Token tidak terduga: " + scalar.token());
        }
    }

    private static void rejectTrailingContent(JsonParser parser, JsonInput input) throws IOException {
        if (parser.nextToken() == null) {
            return;
        }
        throw JacksonErrors.trailing(sourceName(input), parser, offset -> snippet(input, offset));
    }

    private static ErrorSnippet snippet(JsonInput input, long offset) {
        return switch (input) {
            case JsonInput.Text text -> ErrorSnippet.around(text.content(), offset, ErrorSnippet.DEFAULT_RADIUS);
            case JsonInput.FileInput file -> JacksonErrors.fileSnippet(file.file(), offset);
        };
    }

    private static MalformedJsonException malformed(JsonInput input, StreamReadException error) {
        return switch (input) {
            case JsonInput.Text text -> JacksonErrors.fromText(text.content(), error);
            case JsonInput.FileInput file -> JacksonErrors.fromFile(file.file(), error);
        };
    }

    private static String sourceName(JsonInput input) {
        return input instanceof JsonInput.FileInput file
                ? file.file().getFileName().toString()
                : null;
    }

    private static Path inputPath(JsonInput input) {
        return input instanceof JsonInput.FileInput file ? file.file() : Path.of("");
    }

    /** Menghitung key/elemen dan melaporkan progress berkala (sekaligus titik cek pembatalan). */
    private static final class Counter {
        private final long totalBytes;
        private final ScanProgress progress;
        private long keys;
        private long elements;
        private long tokens;

        Counter(long totalBytes, ScanProgress progress) {
            this.totalBytes = totalBytes;
            this.progress = progress;
        }

        void count(JsonParser parser, JsonToken token) {
            if (token == JsonToken.FIELD_NAME) {
                keys++;
            } else if (isInsideArray(parser, token)) {
                elements++;
            }
            if (++tokens % PROGRESS_INTERVAL_TOKENS == 0) {
                progress.update(offset(parser.currentLocation()), totalBytes);
            }
        }

        FormatStats stats() {
            return new FormatStats(keys, elements);
        }

        private static boolean isInsideArray(JsonParser parser, JsonToken token) {
            if (token.isStructEnd()) {
                return false;
            }
            // Untuk START_OBJECT/START_ARRAY, context sudah berpindah ke container baru
            JsonStreamContext context = parser.getParsingContext();
            JsonStreamContext parent = token.isStructStart() ? context.getParent() : context;
            return parent != null && parent.inArray();
        }

        private static long offset(JsonLocation location) {
            return location.getByteOffset() >= 0 ? location.getByteOffset() : location.getCharOffset();
        }
    }

    private sealed interface Node permits ObjectNode, ArrayNode, ScalarNode {}

    private record ObjectNode(Map<String, Node> fields) implements Node {}

    private record ArrayNode(List<Node> items) implements Node {}

    private record ScalarNode(JsonToken token, String text) implements Node {}
}
