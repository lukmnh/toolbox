package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.io.Serial;

/** Gagal mengakses file/folder. Hanya nama file yang disimpan, bukan path lengkap. */
public final class RenameIoException extends RenameException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;

    public RenameIoException(String fileName, Throwable cause) {
        super("Gagal mengakses file", cause);
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
