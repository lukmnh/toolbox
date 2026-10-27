package io.github.lukmnh.toolbox.pdf.shared.domain;

import java.io.Serial;
import java.nio.file.Path;

/** File PDF terenkripsi dan password kosong atau salah. */
public final class PasswordRequiredException extends PdfToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Path file;
    private final boolean passwordWasGiven;

    public PasswordRequiredException(Path file, boolean passwordWasGiven, Throwable cause) {
        super("Password dibutuhkan untuk " + file, cause);
        this.file = file;
        this.passwordWasGiven = passwordWasGiven;
    }

    public Path file() {
        return file;
    }

    /** {@code true} bila password sudah diisi tapi salah. */
    public boolean passwordWasGiven() {
        return passwordWasGiven;
    }
}
