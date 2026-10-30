package io.github.lukmnh.toolbox.common.text;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

/**
 * Menjalankan pekerjaan regex dengan batas waktu (Security Requirements #3). Dipakai Regex Tester dan Batch Rename.
 *
 * <p>Pekerjaan menerima teks yang dibungkus {@link InterruptibleCharSequence}; bila waktu habis
 * {@link RegexTimeoutException} dilempar, bila thread di-interrupt (task dibatalkan)
 * {@link io.github.lukmnh.toolbox.common.task.TaskCancelledException} dilempar.
 */
public final class TimedRegex {

    private TimedRegex() {}

    public static <T> T run(CharSequence text, Duration timeout, Function<InterruptibleCharSequence, T> work) {
        Objects.requireNonNull(text, "text");
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        long deadline = System.nanoTime() + timeout.toNanos();
        return work.apply(new InterruptibleCharSequence(text, deadline, timeout));
    }
}
