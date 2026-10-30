package io.github.lukmnh.toolbox.json.beautify.domain;

/** Batas ukuran dari aturan bisnis BR-JS-2, BR-JS-3, dan BR-JS-4. */
public final class BeautifyLimits {

    private static final long MB = 1024L * 1024L;

    /** Sort keys butuh seluruh dokumen di memori sebagai tree. */
    public static final long SORT_KEYS_MAX_BYTES = 50 * MB;

    public static final int PREVIEW_MAX_BYTES = (int) MB;
    public static final long COPY_MAX_BYTES = 10 * MB;

    private BeautifyLimits() {}

    public static boolean canSortKeys(long inputBytes) {
        return inputBytes <= SORT_KEYS_MAX_BYTES;
    }

    public static boolean canCopy(long outputBytes) {
        return outputBytes <= COPY_MAX_BYTES;
    }
}
