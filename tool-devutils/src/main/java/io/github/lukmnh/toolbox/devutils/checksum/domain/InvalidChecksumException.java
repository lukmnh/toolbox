package io.github.lukmnh.toolbox.devutils.checksum.domain;

import java.io.Serial;

/** Checksum yang ditempel user bukan hex, atau panjangnya tidak cocok dengan algoritma mana pun. */
public final class InvalidChecksumException extends ChecksumException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidChecksumException() {
        super("Checksum tidak valid", null);
    }
}
