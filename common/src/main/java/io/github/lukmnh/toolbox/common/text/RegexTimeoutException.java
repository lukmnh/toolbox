package io.github.lukmnh.toolbox.common.text;

import java.io.Serial;
import java.time.Duration;

/** Eksekusi regex melewati batas waktu, biasanya karena catastrophic backtracking. */
public final class RegexTimeoutException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Duration timeout;

    public RegexTimeoutException(Duration timeout) {
        super("Regex melewati batas waktu " + timeout.toMillis() + " ms");
        this.timeout = timeout;
    }

    public Duration timeout() {
        return timeout;
    }
}
