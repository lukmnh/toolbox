package io.github.lukmnh.toolbox.json.viewer.presentation;

import java.util.AbstractList;
import java.util.RandomAccess;

/**
 * List read-only di atas {@code int[]} nomor record. Item TableView tanpa menyalin 400 ribu {@code Integer}
 * ke ArrayList; boxing hanya terjadi untuk baris yang sedang dirender.
 */
final class IntList extends AbstractList<Integer> implements RandomAccess {

    private final int[] values;

    IntList(int[] values) {
        this.values = values;
    }

    @Override
    public Integer get(int index) {
        return values[index];
    }

    @Override
    public int size() {
        return values.length;
    }
}
