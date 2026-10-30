package io.github.lukmnh.toolbox.json.viewer.domain;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/**
 * Hasil indexing: rentang byte {@code [start, end)} tiap record di file. Isi record tidak disimpan; dibaca
 * ulang dari file saat dibutuhkan.
 */
public final class RecordIndex {

    private final Path file;
    private final JsonFileFormat format;
    private final long fileSize;
    private final LongArrayList starts;
    private final LongArrayList ends;
    private final Duration indexingTime;

    public RecordIndex(
            Path file,
            JsonFileFormat format,
            long fileSize,
            LongArrayList starts,
            LongArrayList ends,
            Duration indexingTime) {
        this.file = Objects.requireNonNull(file, "file");
        this.format = Objects.requireNonNull(format, "format");
        this.fileSize = fileSize;
        this.starts = Objects.requireNonNull(starts, "starts");
        this.ends = Objects.requireNonNull(ends, "ends");
        this.indexingTime = Objects.requireNonNull(indexingTime, "indexingTime");
        if (starts.size() != ends.size()) {
            throw new IllegalArgumentException("Start and end offset counts differ");
        }
    }

    public Path file() {
        return file;
    }

    public JsonFileFormat format() {
        return format;
    }

    public long fileSize() {
        return fileSize;
    }

    public Duration indexingTime() {
        return indexingTime;
    }

    public int recordCount() {
        return starts.size();
    }

    public long start(int record) {
        return starts.get(record);
    }

    public long end(int record) {
        return ends.get(record);
    }

    public long length(int record) {
        return end(record) - start(record);
    }
}
