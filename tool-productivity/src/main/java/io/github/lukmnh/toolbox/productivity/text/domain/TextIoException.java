package io.github.lukmnh.toolbox.productivity.text.domain;

import java.io.Serial;

public final class TextIoException extends TextToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;

    public TextIoException(String fileName, Throwable cause) {
        super("Gagal membaca atau menulis " + fileName, cause);
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
