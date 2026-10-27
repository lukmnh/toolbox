package io.github.lukmnh.toolbox.json.viewer.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** LongArrayList dan LruCache. */
class CollectionsTest {

    @Test
    void longArrayListGrowsBeyondInitialCapacity() {
        LongArrayList list = new LongArrayList(2);
        for (long i = 0; i < 10_000; i++) {
            list.add(i * 3_000_000_000L);
        }
        list.trimToSize();

        assertEquals(10_000, list.size());
        assertEquals(9_999 * 3_000_000_000L, list.get(9_999));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(10_000));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
    }

    @Test
    void emptyLongArrayList() {
        LongArrayList list = new LongArrayList(0);
        assertTrue(list.isEmpty());
        list.add(5);
        assertEquals(5, list.get(0));
    }

    @Test
    void lruCacheEvictsLeastRecentlyUsed() {
        LruCache<Integer, String> cache = new LruCache<>(2);
        cache.put(1, "a");
        cache.put(2, "b");
        cache.get(1);
        cache.put(3, "c");

        assertEquals("a", cache.get(1));
        assertNull(cache.get(2));
        assertEquals("c", cache.get(3));
        assertEquals(2, cache.size());

        cache.clear();
        assertEquals(0, cache.size());
    }

    @Test
    void lruCacheNeedsPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new LruCache<>(0));
    }
}
