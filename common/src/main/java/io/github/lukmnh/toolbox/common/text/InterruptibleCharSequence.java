package io.github.lukmnh.toolbox.common.text;

import io.github.lukmnh.toolbox.common.task.TaskCancelledException;
import java.time.Duration;

/**
 * {@link CharSequence} yang memeriksa deadline dan interrupt di {@link #charAt}. Engine regex tidak bisa dihentikan
 * dari luar, tapi selalu membaca input lewat {@code charAt}, jadi ini satu-satunya titik untuk menghentikan
 * backtracking yang meledak.
 */
public final class InterruptibleCharSequence implements CharSequence {

    /** Cek jam tiap 4.096 pembacaan: overhead kecil, tetap responsif (jauh di bawah 1 ms). */
    private static final int CHECK_MASK = (1 << 12) - 1;

    private final CharSequence inner;
    private final long deadlineNanos;
    private final Duration timeout;
    private int reads;

    InterruptibleCharSequence(CharSequence inner, long deadlineNanos, Duration timeout) {
        this.inner = inner;
        this.deadlineNanos = deadlineNanos;
        this.timeout = timeout;
    }

    @Override
    public char charAt(int index) {
        if ((++reads & CHECK_MASK) == 0) {
            checkBudget();
        }
        return inner.charAt(index);
    }

    @Override
    public int length() {
        return inner.length();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new InterruptibleCharSequence(inner.subSequence(start, end), deadlineNanos, timeout);
    }

    @Override
    public String toString() {
        return inner.toString();
    }

    /** Dipanggil juga oleh pemakai di antara match, supaya loop pencarian panjang ikut terhenti. */
    public void checkBudget() {
        if (Thread.currentThread().isInterrupted()) {
            throw new TaskCancelledException();
        }
        if (System.nanoTime() - deadlineNanos > 0) {
            throw new RegexTimeoutException(timeout);
        }
    }
}
