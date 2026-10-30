package io.github.lukmnh.toolbox.json.csv.domain;

import java.io.Serial;

/** Gagal membaca file sumber atau menulis hasil. Hanya nama file yang disimpan, bukan path lengkap. */
public final class CsvIoException extends CsvToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Arah operasi yang gagal, untuk pesan user. */
    public enum Operation {
        READ,
        WRITE
    }

    private final String fileName;
    private final Operation operation;

    public CsvIoException(String fileName, Operation operation, Throwable cause) {
        super("Could not access file: " + fileName, cause);
        this.fileName = fileName;
        this.operation = operation;
    }

    public String fileName() {
        return fileName;
    }

    public Operation operation() {
        return operation;
    }
}
