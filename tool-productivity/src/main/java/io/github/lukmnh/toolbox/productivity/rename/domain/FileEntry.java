package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Satu entry di folder batch. Atribut dibaca tanpa mengikuti symlink, jadi link diperlakukan sebagai entry sendiri.
 *
 * @param name nama file saja (bukan path); unik di dalam satu folder
 */
public record FileEntry(String name, long sizeBytes, Instant lastModified, boolean symbolicLink) {

    public FileEntry {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(lastModified, "lastModified");
        if (name.isEmpty() || sizeBytes < 0) {
            throw new IllegalArgumentException("Invalid file entry");
        }
    }
}
