package io.github.lukmnh.toolbox.devutils.diff.domain;

/** File biner (gambar, zip, exe) tidak bermakna di-diff per baris, jadi ditolak lebih awal. */
public final class BinaryDetector {

    /** Sama dengan heuristik Git: cek byte NUL di 8 KB pertama. */
    public static final int SNIFF_BYTES = 8 * 1024;

    private BinaryDetector() {}

    public static boolean isBinary(byte[] head, int length) {
        int limit = Math.min(Math.min(length, head.length), SNIFF_BYTES);
        for (int index = 0; index < limit; index++) {
            if (head[index] == 0) {
                return true;
            }
        }
        return false;
    }
}
