package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.io.Serial;

public final class RegexIoException extends RegexToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;

    public RegexIoException(String fileName, Throwable cause) {
        super("Could not read " + fileName, cause);
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
