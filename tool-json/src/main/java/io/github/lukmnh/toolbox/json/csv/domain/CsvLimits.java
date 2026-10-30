package io.github.lukmnh.toolbox.json.csv.domain;

/** Batas kerja tool CSV. Batas ukuran file ada di {@code SecurityLimits.CSV_MAX_FILE_BYTES}. */
public final class CsvLimits {

    /** Sampel awal file untuk deteksi encoding, pemisah, dan header. */
    public static final int SAMPLE_BYTES = 64 * 1024;

    /** Baris data yang ditampilkan di pratinjau input dan hasil. */
    public static final int PREVIEW_ROWS = 200;

    /** Record sampel yang dihitung jumlah kolomnya per kandidat pemisah. */
    public static final int DETECTION_RECORDS = 200;

    /** Entri laporan baris tidak konsisten yang disimpan; sisanya hanya dihitung. */
    public static final int MAX_MISMATCH_ENTRIES = 100;

    /** Cek batal + lapor progress tiap N baris: cukup responsif tanpa overhead per baris. */
    public static final int PROGRESS_INTERVAL_ROWS = 1_000;

    private CsvLimits() {}
}
