package io.github.lukmnh.toolbox.devutils.checksum.application;

import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumAlgorithm;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumFiles;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumIoException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongConsumer;

/** Filesystem in-memory: isi file disimpan sebagai checksum jadi + ukuran, dan setiap file yang dibaca dicatat. */
final class FakeChecksumFiles implements ChecksumFiles {

    /** Ukuran blok yang dilaporkan ke callback progress. */
    static final long BLOCK = 10;

    private final Map<Path, String> checksums = new HashMap<>();
    private final Map<Path, Long> sizes = new HashMap<>();
    private final Map<Path, String> texts = new HashMap<>();
    private final List<Path> read = new ArrayList<>();
    private final List<Path> unreadable = new ArrayList<>();

    FakeChecksumFiles add(Path file, String checksum, long size) {
        checksums.put(file.toAbsolutePath().normalize(), checksum);
        sizes.put(file.toAbsolutePath().normalize(), size);
        return this;
    }

    FakeChecksumFiles addText(Path file, String text) {
        texts.put(file, text);
        return this;
    }

    FakeChecksumFiles unreadable(Path file) {
        unreadable.add(file.toAbsolutePath().normalize());
        return this;
    }

    List<Path> readFiles() {
        return read;
    }

    String written(Path file) {
        return texts.get(file);
    }

    @Override
    public long size(Path file) {
        return sizes.getOrDefault(file.toAbsolutePath().normalize(), 0L);
    }

    @Override
    public boolean isRegularFile(Path file) {
        return checksums.containsKey(file.toAbsolutePath().normalize());
    }

    @Override
    public String checksum(Path file, ChecksumAlgorithm algorithm, LongConsumer bytesRead) {
        Path key = file.toAbsolutePath().normalize();
        read.add(key);
        if (unreadable.contains(key)) {
            throw new ChecksumIoException(file.getFileName().toString(), null);
        }
        for (long done = 0; done < sizes.get(key); done += BLOCK) {
            bytesRead.accept(Math.min(BLOCK, sizes.get(key) - done));
        }
        return checksums.get(key);
    }

    /** Aturan sama dengan {@code SafePaths}, cukup untuk menguji bahwa use case menghormati hasil kosong. */
    @Override
    public Optional<Path> resolveInside(Path directory, String relativeName) {
        Path candidate = directory.resolve(relativeName).normalize();
        boolean absolute = relativeName.startsWith("/") || Path.of(relativeName).isAbsolute();
        return absolute || !candidate.startsWith(directory) ? Optional.empty() : Optional.of(candidate);
    }

    @Override
    public String readSumsText(Path file) {
        return texts.get(file);
    }

    @Override
    public void writeText(Path file, String text) {
        texts.put(file, text);
    }
}
