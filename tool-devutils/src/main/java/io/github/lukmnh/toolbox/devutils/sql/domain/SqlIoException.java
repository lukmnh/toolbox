package io.github.lukmnh.toolbox.devutils.sql.domain;

import java.io.Serial;

public final class SqlIoException extends SqlToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;

    public SqlIoException(String fileName, Throwable cause) {
        super("Could not read or write " + fileName, cause);
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
