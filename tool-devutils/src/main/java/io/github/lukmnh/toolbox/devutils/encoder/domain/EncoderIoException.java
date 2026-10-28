package io.github.lukmnh.toolbox.devutils.encoder.domain;

import java.io.Serial;
import java.nio.file.Path;

/** Gagal membaca atau menulis file. */
public final class EncoderIoException extends EncoderException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Path file;

    public EncoderIoException(Path file, Throwable cause) {
        super("Gagal mengakses file: " + file.getFileName(), cause);
        this.file = file;
    }

    public Path file() {
        return file;
    }
}
