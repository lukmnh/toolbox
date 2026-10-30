package io.github.lukmnh.toolbox.productivity.text.domain;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Riwayat undo transformasi: snapshot teks sebelum tiap transformasi, terbaru di depan.
 *
 * <p>Selain {@link #MAX_STEPS}, total karakter dibatasi {@link #MAX_TOTAL_CHARS}: 20 snapshot teks 10 MB bisa memakan
 * ratusan MB heap. Snapshot terlama dibuang lebih dulu, tapi snapshot terbaru selalu disimpan.
 */
public final class TextHistory {

    public static final int MAX_STEPS = 20;
    public static final long MAX_TOTAL_CHARS = 40_000_000L;

    private final Deque<String> snapshots = new ArrayDeque<>();
    private long totalChars;

    public void push(String snapshot) {
        snapshots.addFirst(snapshot);
        totalChars += snapshot.length();
        while (snapshots.size() > MAX_STEPS || (totalChars > MAX_TOTAL_CHARS && snapshots.size() > 1)) {
            totalChars -= snapshots.removeLast().length();
        }
    }

    /** @return snapshot terbaru (lalu dihapus dari riwayat), kosong bila tidak ada */
    public Optional<String> undo() {
        String latest = snapshots.pollFirst();
        if (latest == null) {
            return Optional.empty();
        }
        totalChars -= latest.length();
        return Optional.of(latest);
    }

    public boolean isEmpty() {
        return snapshots.isEmpty();
    }

    public void clear() {
        snapshots.clear();
        totalChars = 0;
    }
}
