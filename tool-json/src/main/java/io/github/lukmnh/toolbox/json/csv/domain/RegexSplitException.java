package io.github.lukmnh.toolbox.json.csv.domain;

import java.io.Serial;
import java.time.Duration;

/** Split regex dihentikan karena melewati batas waktu atau terlalu kompleks (Security Requirements #3). */
public final class RegexSplitException extends CsvToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Penyebab penghentian. */
    public enum Reason {
        TIMEOUT,
        TOO_COMPLEX
    }

    private final Reason reason;
    private final transient Duration timeout;

    public RegexSplitException(Reason reason, Duration timeout, Throwable cause) {
        super("Regex split stopped: " + reason, cause);
        this.reason = reason;
        this.timeout = timeout;
    }

    public Reason reason() {
        return reason;
    }

    public Duration timeout() {
        return timeout;
    }
}
