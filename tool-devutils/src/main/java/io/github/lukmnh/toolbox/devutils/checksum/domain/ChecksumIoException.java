package io.github.lukmnh.toolbox.devutils.checksum.domain;

import java.io.Serial;

/** Gagal membaca atau menulis file. Hanya nama file yang disimpan, bukan path lengkap (Security Requirements #5). */
public final class ChecksumIoException extends ChecksumException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;

    public ChecksumIoException(String fileName, Throwable cause) {
        super("Could not access file: " + fileName, cause);
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
