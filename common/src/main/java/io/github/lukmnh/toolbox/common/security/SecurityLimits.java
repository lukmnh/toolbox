package io.github.lukmnh.toolbox.common.security;

import java.time.Duration;

/** Batas bersama untuk input tidak dipercaya (Security Requirements #3). Dipakai semua tool. */
public final class SecurityLimits {

    private static final long MB = 1024L * 1024;

    /** PDF di-load penuh ke heap, jadi ukuran file wajib dibatasi. */
    public static final long PDF_MAX_FILE_BYTES = 500 * MB;

    public static final int PDF_MAX_PAGES = 2_000;
    public static final Duration PDF_RENDER_TIMEOUT = Duration.ofSeconds(15);

    /** Dicek dari header gambar sebelum decode (cegah decompression bomb). */
    public static final int IMAGE_MAX_DIMENSION = 10_000;

    public static final long IMAGE_MAX_FILE_BYTES = 50 * MB;

    public static final int JSON_MAX_NESTING_DEPTH = 1_000;
    public static final int JSON_MAX_STRING_LENGTH = (int) (50 * MB);
    public static final int JSON_MAX_NUMBER_LENGTH = 1_000;

    /** File daftar checksum (*SUMS) dibaca utuh ke memori. */
    public static final long CHECKSUM_LIST_MAX_FILE_BYTES = 10 * MB;

    /** Text Diff memuat kedua sisi utuh ke memori untuk algoritma Myers. */
    public static final long DIFF_MAX_FILE_BYTES = 20 * MB;

    /** Teks uji Regex Tester dimuat ke memori dan di-highlight. */
    public static final long REGEX_TEST_MAX_FILE_BYTES = 5 * MB;

    /** SQL Formatter memuat file utuh ke memori (formatter library tidak streaming). */
    public static final long SQL_MAX_FILE_BYTES = 10 * MB;

    /** Text Tools menghitung statistik dari teks utuh di memori. */
    public static final long TEXT_TOOLS_MAX_BYTES = 10 * MB;

    /** CSV diproses streaming; batas ini hanya menjaga waktu proses tetap wajar. */
    public static final long CSV_MAX_FILE_BYTES = 1024 * MB;

    /**
     * Data JSON Budget (file autosave atau data yang tertanam di file export) dibaca utuh lewat databind (Security
     * Requirements #19).
     */
    public static final long BUDGET_IMPORT_MAX_FILE_BYTES = 10 * MB;

    /** File export Budget (.xlsx/.pdf) yang di-import; dicek sebelum dibuka. */
    public static final long BUDGET_EXPORT_MAX_FILE_BYTES = 50 * MB;

    /** Total isi ZIP .xlsx setelah di-decompress; mencegah zip bomb saat import Budget. */
    public static final long BUDGET_XLSX_MAX_UNCOMPRESSED_BYTES = 100 * MB;

    public static final int BUDGET_MAX_ITEMS = 10_000;

    /** Batch Rename per eksekusi (Security Requirements #21). */
    public static final int RENAME_MAX_FILES = 10_000;

    /** Batas eksekusi regex; mencegah catastrophic backtracking membekukan app. */
    public static final Duration REGEX_TIMEOUT = Duration.ofSeconds(2);

    private SecurityLimits() {}
}
