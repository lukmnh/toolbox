package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.io.Serial;

/** Nama file akan keluar dari folder terpilih (Security Requirements #6). */
public final class PathOutsideFolderException extends RenameException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;

    public PathOutsideFolderException(String fileName) {
        super("Path keluar dari folder terpilih", null);
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
