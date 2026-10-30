package io.github.lukmnh.toolbox.json.viewer.domain;

import java.io.Serial;
import java.util.LinkedHashMap;
import java.util.Map;

/** Cache LRU dengan kapasitas tetap. Thread-safe (dipakai FX thread dan task background bersamaan). */
public final class LruCache<K, V> {

    private final BoundedMap<K, V> entries;

    public LruCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be > 0: " + capacity);
        }
        entries = new BoundedMap<>(capacity);
    }

    /** @return nilai, atau {@code null} bila tidak ada */
    public synchronized V get(K key) {
        return entries.get(key);
    }

    public synchronized void put(K key, V value) {
        entries.put(key, value);
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized void clear() {
        entries.clear();
    }

    /** {@link LinkedHashMap} access-order yang membuang entry terlama saat melebihi kapasitas. */
    private static final class BoundedMap<K, V> extends LinkedHashMap<K, V> {

        @Serial
        private static final long serialVersionUID = 1L;

        private static final float LOAD_FACTOR = 0.75f;

        private final int capacity;

        BoundedMap(int capacity) {
            super(16, LOAD_FACTOR, true);
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }
}
