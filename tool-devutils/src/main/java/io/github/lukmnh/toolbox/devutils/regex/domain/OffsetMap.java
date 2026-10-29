package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.Arrays;

/** Memetakan indeks di pola engine (hasil terjemahan/prefix flag) kembali ke indeks di pola asli. */
public final class OffsetMap {

    private final int[] originalOffsets;

    /** @param originalOffsets panjang = panjang pola engine + 1; elemen i = indeks asli untuk karakter engine i */
    public OffsetMap(int[] originalOffsets) {
        this.originalOffsets = Arrays.copyOf(originalOffsets, originalOffsets.length);
    }

    public static OffsetMap identity(int length) {
        int[] offsets = new int[length + 1];
        Arrays.setAll(offsets, index -> index);
        return new OffsetMap(offsets);
    }

    /** Untuk prefix flag {@code (?i)} di depan pola Go: karakter prefix dipetakan ke posisi 0. */
    public static OffsetMap withPrefix(int prefixLength, int patternLength) {
        int[] offsets = new int[prefixLength + patternLength + 1];
        for (int index = 0; index < offsets.length; index++) {
            offsets[index] = Math.max(0, index - prefixLength);
        }
        return new OffsetMap(offsets);
    }

    /** Indeks di luar rentang dijepit ke batas terdekat. */
    public int toOriginal(int engineIndex) {
        if (engineIndex < 0) {
            return 0;
        }
        return originalOffsets[Math.min(engineIndex, originalOffsets.length - 1)];
    }
}
