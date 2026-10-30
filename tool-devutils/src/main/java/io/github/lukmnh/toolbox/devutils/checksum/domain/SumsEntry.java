package io.github.lukmnh.toolbox.devutils.checksum.domain;

import java.util.Objects;

/** Satu baris file daftar checksum. {@code fileName} berasal dari isi file, jadi tidak dipercaya. */
public record SumsEntry(String checksum, String fileName) {

    public SumsEntry {
        Objects.requireNonNull(checksum, "checksum");
        Objects.requireNonNull(fileName, "fileName");
        checksum = ChecksumAlgorithm.normalize(checksum);
        if (!ChecksumAlgorithm.isHex(checksum)) {
            throw new IllegalArgumentException("Checksum must be hex");
        }
        if (fileName.isEmpty()) {
            throw new IllegalArgumentException("Empty file name");
        }
    }
}
