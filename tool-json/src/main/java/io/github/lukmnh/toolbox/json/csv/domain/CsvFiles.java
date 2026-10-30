package io.github.lukmnh.toolbox.json.csv.domain;

import java.nio.file.Path;
import java.util.List;

/** Port: akses file CSV (parser, encoding, file system). */
public interface CsvFiles {

    /** @throws CsvIoException bila file tidak bisa dibaca */
    long size(Path file);

    /** Maksimal {@code maxBytes} byte pertama file. */
    byte[] head(Path file, int maxBytes);

    /**
     * Urai teks sampel dengan {@code delimiter}. Error parse (misalnya quote terbuka karena sampel terpotong)
     * menghentikan penguraian tanpa exception.
     *
     * @return field tiap record, maksimal {@code maxRecords}
     */
    List<List<String>> parseSample(String text, Delimiter delimiter, int maxRecords);

    /** Buka file untuk dibaca record demi record. BOM UTF-8 di awal file dibuang. */
    CsvRecordReader openReader(CsvSource source, InputFormat format);

    /**
     * Buka {@code target} untuk ditulis (isi lama ditimpa).
     *
     * @param displayName nama file untuk pesan error, karena {@code target} biasanya temp file
     */
    CsvRecordWriter openWriter(Path target, String displayName, OutputFormat format);
}
