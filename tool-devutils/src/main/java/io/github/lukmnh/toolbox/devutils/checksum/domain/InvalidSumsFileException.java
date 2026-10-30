package io.github.lukmnh.toolbox.devutils.checksum.domain;

import java.io.Serial;

/** Isi file daftar checksum tidak sesuai format {@code <hash>  <nama-file>}. */
public final class InvalidSumsFileException extends ChecksumException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Penanda bahwa file tidak berisi satu entry pun (bukan nomor baris). */
    public static final int NO_ENTRIES = 0;

    private final int lineNumber;

    public InvalidSumsFileException(int lineNumber) {
        super(lineNumber == NO_ENTRIES ? "No checksum entries" : "Wrong format on line " + lineNumber, null);
        this.lineNumber = lineNumber;
    }

    /** Nomor baris (mulai 1) yang salah, atau {@link #NO_ENTRIES}. */
    public int lineNumber() {
        return lineNumber;
    }
}
