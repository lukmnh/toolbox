package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.Optional;

/** Port: pembaca record streaming. Wajib ditutup (try-with-resources). */
public interface CsvRecordReader extends AutoCloseable {

    /**
     * @return kosong bila file habis
     * @throws CsvEncodingException bila byte tidak valid untuk encoding yang dipilih
     * @throws MalformedCsvException bila file tidak bisa diurai
     * @throws CsvIoException bila gagal membaca
     */
    Optional<CsvRow> next();

    /** Byte file yang sudah dibaca, untuk progress. */
    long bytesRead();

    @Override
    void close();
}
