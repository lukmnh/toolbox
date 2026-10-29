package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.Arrays;

/** Pola Java hasil terjemahan + asal (indeks di pola asli) untuk setiap karakter yang ditulis. */
final class TranslationOutput {

    private static final int INITIAL_CAPACITY = 64;

    private final StringBuilder text = new StringBuilder();
    private int[] origins = new int[INITIAL_CAPACITY];

    void emit(String fragment, int origin) {
        ensureCapacity(text.length() + fragment.length());
        for (int index = 0; index < fragment.length(); index++) {
            origins[text.length() + index] = origin;
        }
        text.append(fragment);
    }

    String text() {
        return text.toString();
    }

    /** @param patternLength panjang pola asli, dipakai untuk posisi "di akhir pola" */
    OffsetMap offsets(int patternLength) {
        int[] mapped = Arrays.copyOf(origins, text.length() + 1);
        mapped[text.length()] = patternLength;
        return new OffsetMap(mapped);
    }

    private void ensureCapacity(int required) {
        if (required >= origins.length) {
            origins = Arrays.copyOf(origins, Math.max(required + 1, origins.length * 2));
        }
    }
}
