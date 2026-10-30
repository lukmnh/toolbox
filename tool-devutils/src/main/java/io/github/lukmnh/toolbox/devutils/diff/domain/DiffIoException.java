package io.github.lukmnh.toolbox.devutils.diff.domain;

import java.io.Serial;

public final class DiffIoException extends DiffException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DiffIoException(String fileName, Throwable cause) {
        super("Could not read or write " + fileName, fileName, cause);
    }
}
