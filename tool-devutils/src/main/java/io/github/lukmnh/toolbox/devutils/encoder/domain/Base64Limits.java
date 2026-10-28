package io.github.lukmnh.toolbox.devutils.encoder.domain;

/** Batas UI mode File: preview dan copy tidak boleh memuat hasil raksasa ke TextArea/clipboard. */
public final class Base64Limits {

    private static final long MB = 1024L * 1024;

    public static final int PREVIEW_BYTES = (int) MB;
    public static final long COPY_MAX_BYTES = 10 * MB;

    private Base64Limits() {}

    public static boolean canCopy(long bytes) {
        return bytes <= COPY_MAX_BYTES;
    }
}
