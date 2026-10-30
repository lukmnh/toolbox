package io.github.lukmnh.toolbox.json.csv.domain;

import java.io.Serial;

/** Isi file tidak valid untuk encoding yang dipilih (misalnya byte Windows-1252 di file yang dibaca sebagai UTF-8). */
public final class CsvEncodingException extends CsvToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;
    private final CsvEncoding encoding;

    public CsvEncodingException(String fileName, CsvEncoding encoding, Throwable cause) {
        super("Encoding tidak cocok: " + fileName, cause);
        this.fileName = fileName;
        this.encoding = encoding;
    }

    public String fileName() {
        return fileName;
    }

    public CsvEncoding encoding() {
        return encoding;
    }
}
