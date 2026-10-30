package io.github.lukmnh.toolbox.json.csv.application;

import io.github.lukmnh.toolbox.json.csv.domain.CsvEncoding;
import io.github.lukmnh.toolbox.json.csv.domain.CsvFileTooLargeException;
import io.github.lukmnh.toolbox.json.csv.domain.CsvFiles;
import io.github.lukmnh.toolbox.json.csv.domain.CsvLimits;
import io.github.lukmnh.toolbox.json.csv.domain.CsvSource;
import io.github.lukmnh.toolbox.json.csv.domain.Delimiter;
import io.github.lukmnh.toolbox.json.csv.domain.DelimiterDetector;
import io.github.lukmnh.toolbox.json.csv.domain.EncodingDetector;
import io.github.lukmnh.toolbox.json.csv.domain.HeaderDetector;
import io.github.lukmnh.toolbox.json.csv.domain.InputFormat;
import io.github.lukmnh.toolbox.json.csv.domain.NotCsvFileException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Terima file CSV: cek ekstensi dan ukuran sebelum dibaca, lalu deteksi encoding, pemisah, dan header. */
public final class OpenCsvFileUseCase {

    /** Dua record pertama cukup untuk heuristik header. */
    private static final int HEADER_SAMPLE_RECORDS = 2;

    private final CsvFiles files;
    private final long maxBytes;

    /** @param maxBytes batas ukuran file (Security Requirements #3) */
    public OpenCsvFileUseCase(CsvFiles files, long maxBytes) {
        this.files = files;
        this.maxBytes = maxBytes;
    }

    /**
     * @throws NotCsvFileException bila ekstensi bukan csv/tsv/txt
     * @throws CsvFileTooLargeException bila melebihi batas ukuran
     */
    public CsvSource execute(Path file) {
        String name = file.getFileName().toString();
        if (!CsvSource.isSupported(file)) {
            throw new NotCsvFileException(name);
        }
        long size = files.size(file);
        if (size > maxBytes) {
            throw new CsvFileTooLargeException(name, size, maxBytes);
        }
        return new CsvSource(file, size, detect(file, size));
    }

    private InputFormat detect(Path file, long size) {
        byte[] head = files.head(file, CsvLimits.SAMPLE_BYTES);
        boolean truncated = size > head.length;
        CsvEncoding encoding = EncodingDetector.detect(head, truncated);
        String text = EncodingDetector.decodeSample(head, encoding, truncated);
        Delimiter delimiter = DelimiterDetector.choose(fieldCounts(text));
        boolean header = HeaderDetector.looksLikeHeader(files.parseSample(text, delimiter, HEADER_SAMPLE_RECORDS));
        return new InputFormat(delimiter, encoding, header);
    }

    private Map<Delimiter, List<Integer>> fieldCounts(String text) {
        Map<Delimiter, List<Integer>> counts = new EnumMap<>(Delimiter.class);
        for (Delimiter candidate : Delimiter.values()) {
            counts.put(
                    candidate,
                    files.parseSample(text, candidate, CsvLimits.DETECTION_RECORDS).stream()
                            .map(List::size)
                            .toList());
        }
        return counts;
    }
}
