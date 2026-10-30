package io.github.lukmnh.toolbox.json.csv.domain;

import java.io.Serial;

/** Ekstensi file bukan {@code .csv}, {@code .tsv}, atau {@code .txt}. */
public final class NotCsvFileException extends CsvToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;

    public NotCsvFileException(String fileName) {
        super("Bukan file CSV: " + fileName, null);
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
