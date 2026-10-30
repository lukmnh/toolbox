package io.github.lukmnh.toolbox.json.csv.domain;

import java.io.Serial;

/**
 * Semua error domain tool CSV. Sealed supaya presentation bisa memetakan setiap jenis ke pesan user dengan
 * {@code switch} yang exhaustive. Pesan exception tidak pernah berisi isi CSV (Security Requirements #5).
 */
public abstract sealed class CsvToolException extends RuntimeException
        permits CsvIoException,
                CsvFileTooLargeException,
                NotCsvFileException,
                CsvEncodingException,
                MalformedCsvException,
                InvalidStepException,
                RegexSplitException {

    @Serial
    private static final long serialVersionUID = 1L;

    protected CsvToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
