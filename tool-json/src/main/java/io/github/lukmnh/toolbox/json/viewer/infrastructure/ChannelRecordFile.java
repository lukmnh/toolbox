package io.github.lukmnh.toolbox.json.viewer.infrastructure;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import io.github.lukmnh.toolbox.json.shared.domain.JsonIoException;
import io.github.lukmnh.toolbox.json.shared.infrastructure.JacksonErrors;
import io.github.lukmnh.toolbox.json.shared.infrastructure.JsonFactories;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonNodeInfo;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonValue;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonWriter;
import io.github.lukmnh.toolbox.json.viewer.domain.NodeChildren;
import io.github.lukmnh.toolbox.json.viewer.domain.NodeType;
import io.github.lukmnh.toolbox.json.viewer.domain.RecordFile;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Akses record lewat {@code FileChannel.read(buffer, position)}: aman untuk file > 2 GB dan multi-thread. */
final class ChannelRecordFile implements RecordFile {

    private final Path file;
    private final FileChannel channel;

    ChannelRecordFile(Path file, FileChannel channel) {
        this.file = file;
        this.channel = channel;
    }

    @Override
    public byte[] read(long offset, int length) {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        try {
            while (buffer.hasRemaining()) {
                if (channel.read(buffer, offset + buffer.position()) < 0) {
                    throw new EOFException("File changed or truncated: " + file);
                }
            }
            return buffer.array();
        } catch (IOException e) {
            throw new JsonIoException(file, e);
        }
    }

    @Override
    public JsonValue parse(byte[] raw) {
        try (JsonParser parser = JsonFactories.FACTORY.createParser(raw)) {
            if (parser.nextToken() == null) {
                throw JacksonErrors.empty(null);
            }
            JsonValue value = JacksonValueReader.read(parser);
            if (parser.nextToken() != null) {
                throw JacksonErrors.trailing(null, parser, offset -> JacksonErrors.bytesSnippet(raw, offset));
            }
            return value;
        } catch (StreamReadException e) {
            throw JacksonErrors.fromBytes(null, raw, e);
        } catch (StreamConstraintsException e) {
            throw JacksonErrors.limitExceeded(null, e);
        } catch (IOException e) {
            throw new JsonIoException(file, e);
        }
    }

    @Override
    public JsonNodeInfo node(long start, long end) {
        try (JsonParser parser = parser(start, end)) {
            JsonToken token = parser.nextToken();
            if (token == null) {
                throw JacksonErrors.empty(file.getFileName().toString());
            }
            return describe(parser, token, null, -1, start);
        } catch (StreamReadException e) {
            throw JacksonErrors.fromFile(file, e);
        } catch (StreamConstraintsException e) {
            throw JacksonErrors.limitExceeded(file.getFileName().toString(), e);
        } catch (IOException e) {
            throw new JsonIoException(file, e);
        }
    }

    @Override
    public NodeChildren children(long start, long end, int maxChildren) {
        try (JsonParser parser = parser(start, end)) {
            JsonToken container = parser.nextToken();
            if (container == null || !container.isStructStart()) {
                return new NodeChildren(List.of(), 0);
            }
            boolean object = container == JsonToken.START_OBJECT;
            List<JsonNodeInfo> nodes = new ArrayList<>();
            int total = 0;
            JsonToken token = parser.nextToken();
            while (token != null && !token.isStructEnd()) {
                String key = null;
                if (object) {
                    key = parser.currentName();
                    token = parser.nextToken();
                }
                if (total < maxChildren) {
                    nodes.add(describe(parser, token, key, object ? -1 : total, start));
                } else if (token.isStructStart()) {
                    parser.skipChildren();
                }
                total++;
                token = parser.nextToken();
            }
            return new NodeChildren(nodes, total);
        } catch (StreamReadException e) {
            throw JacksonErrors.fromFile(file, e);
        } catch (StreamConstraintsException e) {
            throw JacksonErrors.limitExceeded(file.getFileName().toString(), e);
        } catch (IOException e) {
            throw new JsonIoException(file, e);
        }
    }

    @Override
    public void close() {
        try {
            channel.close();
        } catch (IOException e) {
            throw new JsonIoException(file, e);
        }
    }

    private JsonParser parser(long start, long end) throws IOException {
        return JsonFactories.FACTORY.createParser(new RangeInputStream(channel, start, end));
    }

    /** Parser ada di token awal nilai; setelahnya berada di token terakhir nilai tersebut. */
    private static JsonNodeInfo describe(JsonParser parser, JsonToken token, String key, int index, long base)
            throws IOException {
        long start = base + parser.currentTokenLocation().getByteOffset();
        NodeType type = typeOf(token);
        if (token.isStructStart()) {
            int count = countChildren(parser);
            return new JsonNodeInfo(
                    key,
                    index,
                    type,
                    "",
                    count,
                    start,
                    base + parser.currentLocation().getByteOffset());
        }
        String preview = JsonWriter.truncate(parser.getText(), JsonNodeInfo.PREVIEW_MAX_CHARS);
        return new JsonNodeInfo(
                key,
                index,
                type,
                preview,
                0,
                start,
                base + parser.currentLocation().getByteOffset());
    }

    private static int countChildren(JsonParser parser) throws IOException {
        int count = 0;
        JsonToken token = parser.nextToken();
        while (token != null && !token.isStructEnd()) {
            if (token != JsonToken.FIELD_NAME) {
                if (token.isStructStart()) {
                    parser.skipChildren();
                }
                count++;
            }
            token = parser.nextToken();
        }
        return count;
    }

    private static NodeType typeOf(JsonToken token) {
        return switch (token) {
            case START_OBJECT -> NodeType.OBJECT;
            case START_ARRAY -> NodeType.ARRAY;
            case VALUE_STRING -> NodeType.STRING;
            case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> NodeType.NUMBER;
            case VALUE_TRUE, VALUE_FALSE -> NodeType.BOOLEAN;
            case VALUE_NULL -> NodeType.NULL;
            default -> throw new IllegalStateException("Unexpected token: " + token);
        };
    }
}
