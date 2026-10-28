package io.github.lukmnh.toolbox.json.viewer.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.json.shared.domain.MalformedJsonException;
import io.github.lukmnh.toolbox.json.shared.domain.ScanProgress;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonFileFormat;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonNodeInfo;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonValue;
import io.github.lukmnh.toolbox.json.viewer.domain.NodeChildren;
import io.github.lukmnh.toolbox.json.viewer.domain.NodeType;
import io.github.lukmnh.toolbox.json.viewer.domain.RecordFile;
import io.github.lukmnh.toolbox.json.viewer.domain.RecordIndex;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JacksonJsonFilesTest {

    private final JacksonJsonFiles files = new JacksonJsonFiles();

    @TempDir
    Path temp;

    @Test
    void indexesArrayElementsOfEveryTypeWithExactByteRanges() throws IOException {
        Path file = write(
                "data.json",
                "[\n  {\"a\": 1},\n  22 ,\"x]y\" , true,null,-1.5e3,\n"
                        + "  {\"s\":\"{}[],\\\"\\\\\"},\n  [1,[2]]\n]\n");

        RecordIndex index = files.index(file, ScanProgress.NONE);

        assertEquals(JsonFileFormat.ARRAY, index.format());
        assertEquals(
                List.of(
                        "{\"a\": 1}",
                        "22",
                        "\"x]y\"",
                        "true",
                        "null",
                        "-1.5e3",
                        "{\"s\":\"{}[],\\\"\\\\\"}",
                        "[1,[2]]"),
                rawRecords(file, index));
    }

    @Test
    void emptyArrayHasNoRecords() throws IOException {
        RecordIndex index = files.index(write("kosong.json", " [ ] "), ScanProgress.NONE);

        assertEquals(JsonFileFormat.ARRAY, index.format());
        assertEquals(0, index.recordCount());
    }

    @Test
    void utf8BomIsSkippedAndOffsetsStayAbsolute() throws IOException {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = "[{\"nama\":\"Ani\"},{\"nama\":\"Budi\"}]".getBytes(StandardCharsets.UTF_8);
        Path file = temp.resolve("bom.json");
        Files.write(file, concat(bom, body));

        RecordIndex index = files.index(file, ScanProgress.NONE);

        assertEquals(List.of("{\"nama\":\"Ani\"}", "{\"nama\":\"Budi\"}"), rawRecords(file, index));
    }

    @Test
    void unicodeAndEmojiKeepCorrectOffsets() throws IOException {
        Path file = write("emoji.json", "[{\"n\":\"é😀日本\"},{\"n\":\"z\"}]");

        RecordIndex index = files.index(file, ScanProgress.NONE);

        assertEquals(List.of("{\"n\":\"é😀日本\"}", "{\"n\":\"z\"}"), rawRecords(file, index));
        try (RecordFile records = files.open(file)) {
            JsonValue first = records.parse(records.read(index.start(0), (int) index.length(0)));
            assertEquals(
                    new JsonValue.JsonString("é😀日本"),
                    ((JsonValue.JsonObject) first).fields().get("n"));
        }
    }

    @Test
    void jsonLinesSkipBlankLinesAndTrimCarriageReturn() throws IOException {
        Path file = write("log.jsonl", "{\"a\":1}\r\n\r\n  {\"a\":2}  \n{\"a\":3}");

        RecordIndex index = files.index(file, ScanProgress.NONE);

        assertEquals(JsonFileFormat.JSON_LINES, index.format());
        assertEquals(List.of("{\"a\":1}", "{\"a\":2}", "{\"a\":3}"), rawRecords(file, index));
    }

    @Test
    void ndjsonEmptyFileIsValidWithZeroRecords() throws IOException {
        RecordIndex index = files.index(write("kosong.ndjson", "\n\n"), ScanProgress.NONE);

        assertEquals(0, index.recordCount());
    }

    @Test
    void jsonFileWithSeveralRootValuesIsTreatedAsJsonLines() throws IOException {
        Path file = write("dump.json", "{\"id\":1}\n{\"id\":2}\n");

        RecordIndex index = files.index(file, ScanProgress.NONE);

        assertEquals(JsonFileFormat.JSON_LINES, index.format());
        assertEquals(List.of("{\"id\":1}", "{\"id\":2}"), rawRecords(file, index));
    }

    @Test
    void singleRootObjectOpensInTreeMode() throws IOException {
        Path file =
                write("config.json", "\n{\"server\": {\"port\": 8080, \"hosts\": [\"a\", \"b\"]}, \"debug\": false}\n");

        RecordIndex index = files.index(file, ScanProgress.NONE);

        assertEquals(JsonFileFormat.SINGLE_VALUE, index.format());
        assertEquals(1, index.recordCount());
        try (RecordFile records = files.open(file)) {
            JsonNodeInfo root = records.node(index.start(0), index.end(0));
            assertEquals(NodeType.OBJECT, root.type());
            assertEquals(2, root.childCount());

            NodeChildren top = records.children(root.start(), root.end(), 100);
            assertEquals(
                    List.of("server", "debug"),
                    top.nodes().stream().map(JsonNodeInfo::key).toList());
            JsonNodeInfo server = top.nodes().getFirst();
            assertEquals(2, server.childCount());
            assertEquals("false", top.nodes().get(1).preview());

            NodeChildren serverChildren = records.children(server.start(), server.end(), 100);
            JsonNodeInfo hosts = serverChildren.nodes().get(1);
            assertEquals(NodeType.ARRAY, hosts.type());
            NodeChildren hostItems = records.children(hosts.start(), hosts.end(), 1);
            assertEquals(1, hostItems.nodes().size());
            assertEquals(2, hostItems.totalCount());
            assertEquals(1, hostItems.hiddenCount());
            assertEquals(0, hostItems.nodes().getFirst().index());
        }
    }

    @Test
    void brokenJsonReportsLineColumnAndSnippet() throws IOException {
        Path file = write("rusak.json", "[\n  {\"a\": 1},\n  {\"b\": }\n]");

        MalformedJsonException error =
                assertThrows(MalformedJsonException.class, () -> files.index(file, ScanProgress.NONE));

        assertEquals(MalformedJsonException.Kind.SYNTAX, error.kind());
        assertEquals(3, error.line());
        assertEquals(9, error.column());
        assertEquals("rusak.json", error.sourceName().orElseThrow());
        assertEquals('}', error.snippet().text().charAt(error.snippet().marker()));
    }

    @Test
    void contentAfterArrayIsRejected() throws IOException {
        MalformedJsonException error = assertThrows(
                MalformedJsonException.class, () -> files.index(write("dua.json", "[1]\n[2]"), ScanProgress.NONE));

        assertEquals(MalformedJsonException.Kind.TRAILING_CONTENT, error.kind());
        assertEquals(2, error.line());
    }

    @Test
    void emptyJsonFileIsRejected() throws IOException {
        MalformedJsonException error = assertThrows(
                MalformedJsonException.class, () -> files.index(write("kosong.json", "  \n"), ScanProgress.NONE));

        assertEquals(MalformedJsonException.Kind.EMPTY, error.kind());
    }

    @Test
    void progressReachesFileSize() throws IOException {
        Path file = write("p.json", "[1,2,3]");
        List<long[]> updates = new ArrayList<>();

        files.index(file, (done, total) -> updates.add(new long[] {done, total}));

        long[] last = updates.getLast();
        assertEquals(Files.size(file), last[0]);
        assertEquals(last[1], last[0]);
    }

    @Test
    void openFileDoesNotLockSource() throws IOException {
        Path file = write("lock.json", "[1]");
        try (RecordFile ignored = files.open(file)) {
            Path moved = temp.resolve("moved.json");
            Files.move(file, moved);
            assertTrue(Files.exists(moved));
            assertFalse(Files.exists(file));
        }
    }

    @Test
    void deepNestingIsRejectedAsLimitNotIoError() throws IOException {
        Path file = write("dalam.json", "{\"a\":" + "[".repeat(100_000) + "]".repeat(100_000) + "}");

        MalformedJsonException error =
                assertThrows(MalformedJsonException.class, () -> files.index(file, ScanProgress.NONE));

        assertEquals(MalformedJsonException.Kind.LIMIT_EXCEEDED, error.kind());
    }

    private Path write(String name, String content) throws IOException {
        return Files.writeString(temp.resolve(name), content);
    }

    private List<String> rawRecords(Path file, RecordIndex index) {
        List<String> records = new ArrayList<>();
        try (RecordFile source = files.open(file)) {
            for (int i = 0; i < index.recordCount(); i++) {
                records.add(new String(source.read(index.start(i), (int) index.length(i)), StandardCharsets.UTF_8));
            }
        }
        return records;
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] all = new byte[first.length + second.length];
        System.arraycopy(first, 0, all, 0, first.length);
        System.arraycopy(second, 0, all, first.length, second.length);
        return all;
    }
}
