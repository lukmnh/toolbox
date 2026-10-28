package io.github.lukmnh.toolbox.devutils.diff.domain;

import java.io.Serial;

public final class BinaryFileException extends DiffException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BinaryFileException(String fileName) {
        super("File biner tidak bisa dibandingkan: " + fileName, fileName, null);
    }
}
