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

    private SecurityLimits() {}
}
