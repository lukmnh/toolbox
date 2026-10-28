package io.github.lukmnh.toolbox.devutils.checksum.domain;

import java.time.Duration;
import java.util.Objects;

/** Hasil checksum satu file. */
public record FileChecksum(String fileName, long sizeBytes, String checksum, Duration duration) {

    public FileChecksum {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(checksum, "checksum");
        Objects.requireNonNull(duration, "duration");
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("Ukuran negatif");
        }
    }

    public SumsEntry toSumsEntry() {
        return new SumsEntry(checksum, fileName);
    }
}
