package io.github.lukmnh.toolbox.json.csv.domain;

import java.io.Serial;

/** File tidak bisa diurai sebagai CSV, atau tidak berisi data sama sekali. */
public final class MalformedCsvException extends CsvToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Jenis kerusakan, untuk pesan user. */
    public enum Kind {
        EMPTY,
        UNPARSEABLE
    }

    private final String fileName;
    private final Kind kind;

    public MalformedCsvException(String fileName, Kind kind, Throwable cause) {
        super("CSV rusak: " + fileName, cause);
        this.fileName = fileName;
        this.kind = kind;
    }

    public String fileName() {
        return fileName;
    }

    public Kind kind() {
        return kind;
    }
}
