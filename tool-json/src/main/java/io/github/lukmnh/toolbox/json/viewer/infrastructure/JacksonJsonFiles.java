package io.github.lukmnh.toolbox.json.viewer.infrastructure;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import io.github.lukmnh.toolbox.json.shared.domain.JsonIoException;
import io.github.lukmnh.toolbox.json.shared.domain.ScanProgress;
import io.github.lukmnh.toolbox.json.shared.infrastructure.JacksonErrors;
import io.github.lukmnh.toolbox.json.shared.infrastructure.JsonFactories;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonFileFormat;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonFiles;
import io.github.lukmnh.toolbox.json.viewer.domain.LongArrayList;
import io.github.lukmnh.toolbox.json.viewer.domain.RecordFile;
import io.github.lukmnh.toolbox.json.viewer.domain.RecordIndex;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/**
 * Indexer streaming. Array top-level: offset tiap elemen dari {@link JsonParser} + {@code skipChildren()}.
 * JSONL/NDJSON: scan byte {@code \n}. Isi record tidak pernah disimpan, hanya rentang byte-nya.
 */
public final class JacksonJsonFiles implements JsonFiles {

    private static final Set<String> LINE_EXTENSIONS = Set.of("jsonl", "ndjson");
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final int SCAN_BUFFER_BYTES = 1 << 20;
    private static final int PROGRESS_INTERVAL_RECORDS = 4096;

    @Override
    public RecordIndex index(Path file, ScanProgress progress) {
        long started = System.nanoTime();
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            long size = channel.size();
            long contentStart = firstContentByte(channel, size);
            Offsets offsets = new Offsets();
            JsonFileFormat format;
            if (LINE_EXTENSIONS.contains(extension(file))) {
                indexLines(channel, contentStart, size, offsets, progress);
                format = JsonFileFormat.JSON_LINES;
            } else if (contentStart == size) {
                throw JacksonErrors.empty(file.getFileName().toString());
            } else if (byteAt(channel, contentStart) == '[') {
                indexArray(file, size, offsets, progress);
                format = JsonFileFormat.ARRAY;
            } else {
                int roots = indexRootValues(file, size, offsets, progress);
                // Beberapa nilai root berturut-turut = JSONL yang disimpan dengan ekstensi .json
                format = roots == 1 ? JsonFileFormat.SINGLE_VALUE : JsonFileFormat.JSON_LINES;
            }
            progress.update(size, size);
            Duration elapsed = Duration.ofNanos(System.nanoTime() - started);
            return new RecordIndex(file, format, size, offsets.starts(), offsets.ends(), elapsed);
        } catch (StreamReadException e) {
            throw JacksonErrors.fromFile(file, e);
        } catch (StreamConstraintsException e) {
            throw JacksonErrors.limitExceeded(file.getFileName().toString(), e);
        } catch (IOException e) {
            throw new JsonIoException(file, e);
        }
    }

    @Override
    public RecordFile open(Path file) {
        try {
            return new ChannelRecordFile(file, FileChannel.open(file, StandardOpenOption.READ));
        } catch (IOException e) {
            throw new JsonIoException(file, e);
        }
    }

    private static void indexArray(Path file, long size, Offsets offsets, ScanProgress progress) throws IOException {
        try (JsonParser parser = JsonFactories.FACTORY.createParser(Files.newInputStream(file))) {
            parser.nextToken();
            JsonToken token = parser.nextToken();
            while (token != null && token != JsonToken.END_ARRAY) {
                long start = parser.currentTokenLocation().getByteOffset();
                skipValue(parser, token);
                offsets.add(start, parser.currentLocation().getByteOffset());
                reportEvery(offsets, parser, size, progress);
                token = parser.nextToken();
            }
            rejectTrailingContent(file, parser);
        }
    }

    /** @return jumlah nilai root */
    private static int indexRootValues(Path file, long size, Offsets offsets, ScanProgress progress)
            throws IOException {
        try (JsonParser parser = JsonFactories.FACTORY.createParser(Files.newInputStream(file))) {
            JsonToken token = parser.nextToken();
            while (token != null) {
                long start = parser.currentTokenLocation().getByteOffset();
                skipValue(parser, token);
                offsets.add(start, parser.currentLocation().getByteOffset());
                reportEvery(offsets, parser, size, progress);
                token = parser.nextToken();
            }
            return offsets.size();
        }
    }

    /** Tiap baris yang tidak kosong = satu record; spasi, tab, dan {@code \r} di tepi baris diabaikan. */
    private static void indexLines(
            FileChannel channel, long contentStart, long size, Offsets offsets, ScanProgress progress)
            throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(SCAN_BUFFER_BYTES);
        LineTracker line = new LineTracker(offsets);
        long position = contentStart;
        while (position < size) {
            buffer.clear();
            int read = channel.read(buffer, position);
            if (read < 0) {
                break;
            }
            for (int i = 0; i < read; i++) {
                line.accept(buffer.get(i), position + i);
            }
            position += read;
            progress.update(position, size);
        }
        line.finish();
    }

    private static void skipValue(JsonParser parser, JsonToken token) throws IOException {
        if (token.isStructStart()) {
            parser.skipChildren();
        } else {
            // String dibaca lazy oleh Jackson; selesaikan supaya lokasi akhir tepat setelah kutip penutup
            parser.finishToken();
        }
    }

    private static void reportEvery(Offsets offsets, JsonParser parser, long size, ScanProgress progress) {
        if (offsets.size() % PROGRESS_INTERVAL_RECORDS == 0) {
            progress.update(parser.currentLocation().getByteOffset(), size);
        }
    }

    private static void rejectTrailingContent(Path file, JsonParser parser) throws IOException {
        if (parser.nextToken() != null) {
            throw JacksonErrors.trailing(
                    file.getFileName().toString(), parser, offset -> JacksonErrors.fileSnippet(file, offset));
        }
    }

    /** Posisi byte pertama setelah BOM UTF-8 dan whitespace; {@code size} bila file kosong. */
    private static long firstContentByte(FileChannel channel, long size) throws IOException {
        long position = hasBom(channel, size) ? UTF8_BOM.length : 0;
        while (position < size && isWhitespace(byteAt(channel, position))) {
            position++;
        }
        return position;
    }

    private static boolean hasBom(FileChannel channel, long size) throws IOException {
        if (size < UTF8_BOM.length) {
            return false;
        }
        for (int i = 0; i < UTF8_BOM.length; i++) {
            if (byteAt(channel, i) != UTF8_BOM[i]) {
                return false;
            }
        }
        return true;
    }

    private static byte byteAt(FileChannel channel, long position) throws IOException {
        ByteBuffer single = ByteBuffer.allocate(1);
        channel.read(single, position);
        return single.get(0);
    }

    private static boolean isWhitespace(byte value) {
        return value == ' ' || value == '\t' || value == '\r' || value == '\n';
    }

    private static String extension(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static final class Offsets {
        private final LongArrayList starts = new LongArrayList();
        private final LongArrayList ends = new LongArrayList();

        void add(long start, long end) {
            starts.add(start);
            ends.add(end);
        }

        int size() {
            return starts.size();
        }

        LongArrayList starts() {
            starts.trimToSize();
            return starts;
        }

        LongArrayList ends() {
            ends.trimToSize();
            return ends;
        }
    }

    /** Mencatat byte non-whitespace pertama dan terakhir di baris yang sedang dipindai. */
    private static final class LineTracker {
        private final Offsets offsets;
        private long first = -1;
        private long last = -1;

        LineTracker(Offsets offsets) {
            this.offsets = offsets;
        }

        void accept(byte value, long position) {
            if (value == '\n') {
                finish();
            } else if (value != ' ' && value != '\t' && value != '\r') {
                if (first < 0) {
                    first = position;
                }
                last = position;
            }
        }

        void finish() {
            if (first >= 0) {
                offsets.add(first, last + 1);
            }
            first = -1;
            last = -1;
        }
    }
}
