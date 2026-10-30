package io.github.lukmnh.toolbox.json.csv.domain;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** File CSV yang sudah lolos cek ukuran, beserta format hasil deteksi. */
public record CsvSource(Path file, long sizeBytes, InputFormat detected) {

    /** Ekstensi yang diterima (huruf kecil, tanpa titik). */
    public static final Set<String> EXTENSIONS = Set.of("csv", "tsv", "txt");

    private static final String RESULT_SUFFIX = "_hasil.csv";

    public CsvSource {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(detected, "detected");
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("Ukuran file negatif");
        }
    }

    public String fileName() {
        return file.getFileName().toString();
    }

    /** {@code data.csv} → {@code data_hasil.csv}. */
    public String resultFileName() {
        String name = fileName();
        int dot = name.lastIndexOf('.');
        return (dot <= 0 ? name : name.substring(0, dot)) + RESULT_SUFFIX;
    }

    public static boolean isSupported(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 && EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }
}
