package io.github.lukmnh.toolbox.json.viewer.application;

import io.github.lukmnh.toolbox.json.shared.domain.Utf8Text;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonFileFormat;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonNodeInfo;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonValue;
import io.github.lukmnh.toolbox.json.viewer.domain.LruCache;
import io.github.lukmnh.toolbox.json.viewer.domain.NodeChildren;
import io.github.lukmnh.toolbox.json.viewer.domain.RecordFile;
import io.github.lukmnh.toolbox.json.viewer.domain.RecordIndex;
import io.github.lukmnh.toolbox.json.viewer.domain.RecordSource;
import io.github.lukmnh.toolbox.json.viewer.domain.TableColumns;

/**
 * File JSON yang sedang dibuka di Viewer: index offset + file terbuka + cache record ter-parse. Thread-safe,
 * dipakai bersamaan oleh FX thread (render tabel) dan task background (filter, sort, export).
 */
public final class ViewerSession implements RecordSource, AutoCloseable {

    /** Cukup untuk beberapa layar tabel + scroll bolak-balik, tetap kecil di memori. */
    public static final int CACHE_CAPACITY = 2_000;
    /** Anak node tree yang ditampilkan per level; sisanya hanya dihitung. */
    public static final int TREE_CHILD_LIMIT = 5_000;

    private static final int MAX_RECORD_BYTES = Integer.MAX_VALUE - 8;

    private final RecordIndex index;
    private final RecordFile file;
    private final TableColumns columns;
    private final LruCache<Integer, JsonValue> cache = new LruCache<>(CACHE_CAPACITY);

    public ViewerSession(RecordIndex index, RecordFile file, TableColumns columns) {
        this.index = index;
        this.file = file;
        this.columns = columns;
    }

    public RecordIndex index() {
        return index;
    }

    public JsonFileFormat format() {
        return index.format();
    }

    public TableColumns columns() {
        return columns;
    }

    public int recordCount() {
        return index.recordCount();
    }

    /** Nomor semua record sesuai urutan file (view tanpa filter/sort). */
    public int[] allRows() {
        int[] rows = new int[recordCount()];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = i;
        }
        return rows;
    }

    /** Byte mentah record, dibaca langsung dari file (tanpa cache). */
    @Override
    public byte[] raw(int record) {
        long length = index.length(record);
        if (length > MAX_RECORD_BYTES) {
            throw new IllegalStateException("Record #" + (record + 1) + " is larger than 2 GB");
        }
        return file.read(index.start(record), (int) length);
    }

    /** Teks mentah maksimal {@code maxBytes} pertama, tanpa memotong karakter UTF-8. */
    public String rawPrefix(int record, int maxBytes) {
        int length = (int) Math.min(index.length(record), maxBytes + 1L);
        return Utf8Text.decodePrefix(file.read(index.start(record), length), maxBytes);
    }

    @Override
    public JsonValue parse(byte[] raw) {
        return file.parse(raw);
    }

    /** Record ter-parse, dari cache LRU bila ada. */
    public JsonValue record(int record) {
        JsonValue cached = cache.get(record);
        if (cached != null) {
            return cached;
        }
        JsonValue value = parse(raw(record));
        cache.put(record, value);
        return value;
    }

    public JsonNodeInfo recordNode(int record) {
        return file.node(index.start(record), index.end(record));
    }

    public NodeChildren children(JsonNodeInfo node) {
        return file.children(node.start(), node.end(), TREE_CHILD_LIMIT);
    }

    @Override
    public void close() {
        cache.clear();
        file.close();
    }
}
