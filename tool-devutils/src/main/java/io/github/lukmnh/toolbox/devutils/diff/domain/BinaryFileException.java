package io.github.lukmnh.toolbox.devutils.diff.domain;

import java.io.Serial;

public final class BinaryFileException extends DiffException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BinaryFileException(String fileName) {
        super("Binary file cannot be compared: " + fileName, fileName, null);
    }
}
