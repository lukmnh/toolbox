package io.github.lukmnh.toolbox.productivity.text.domain;

import java.io.Serial;

public final class TextIoException extends TextToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;

    public TextIoException(String fileName, Throwable cause) {
        super("Could not read or write " + fileName, cause);
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
