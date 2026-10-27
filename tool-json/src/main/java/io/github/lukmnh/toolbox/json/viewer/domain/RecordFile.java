package io.github.lukmnh.toolbox.json.viewer.domain;

/** Port: file JSON yang terbuka. Semua method thread-safe (dipanggil FX thread dan task background). */
public interface RecordFile extends AutoCloseable {

    byte[] read(long offset, int length);

    /** @throws io.github.lukmnh.toolbox.json.shared.domain.MalformedJsonException bila byte bukan JSON valid */
    JsonValue parse(byte[] raw);

    /** Info node untuk nilai JSON di rentang {@code [start, end)}. */
    JsonNodeInfo node(long start, long end);

    /** Anak langsung dari object/array di rentang {@code [start, end)}, maksimal {@code maxChildren}. */
    NodeChildren children(long start, long end, int maxChildren);

    @Override
    void close();
}
