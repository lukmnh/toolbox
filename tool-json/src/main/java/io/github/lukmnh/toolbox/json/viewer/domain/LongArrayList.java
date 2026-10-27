package io.github.lukmnh.toolbox.json.viewer.domain;

import java.util.Arrays;

/** List {@code long} primitif yang bisa tumbuh. 400 ribu offset = ~3 MB, bukan ~16 MB seperti {@code List<Long>}. */
public final class LongArrayList {

    private static final int DEFAULT_CAPACITY = 1024;
    /** Batas aman ukuran array JVM. */
    private static final int MAX_CAPACITY = Integer.MAX_VALUE - 8;

    private long[] values;
    private int size;

    public LongArrayList() {
        this(DEFAULT_CAPACITY);
    }

    public LongArrayList(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Kapasitas negatif: " + initialCapacity);
        }
        values = new long[initialCapacity];
    }

    public void add(long value) {
        if (size == values.length) {
            grow();
        }
        values[size++] = value;
    }

    public long get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index " + index + ", size " + size);
        }
        return values[index];
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /** Lepas kapasitas cadangan setelah indexing selesai. */
    public void trimToSize() {
        if (values.length != size) {
            values = Arrays.copyOf(values, size);
        }
    }

    private void grow() {
        if (values.length == MAX_CAPACITY) {
            throw new IllegalStateException("LongArrayList penuh");
        }
        long grown = Math.max(DEFAULT_CAPACITY, values.length + ((long) values.length >> 1));
        values = Arrays.copyOf(values, (int) Math.min(grown, MAX_CAPACITY));
    }
}
