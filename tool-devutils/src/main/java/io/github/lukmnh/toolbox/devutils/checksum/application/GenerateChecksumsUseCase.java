package io.github.lukmnh.toolbox.devutils.checksum.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumAlgorithm;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumFiles;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumIoException;
import io.github.lukmnh.toolbox.devutils.checksum.domain.FileChecksum;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Hitung checksum banyak file berurutan, dengan progress total (byte) dan bisa dibatalkan. */
public final class GenerateChecksumsUseCase {

    private final ChecksumFiles files;

    public GenerateChecksumsUseCase(ChecksumFiles files) {
        this.files = files;
    }

    /**
     * @return satu hasil per file, sesuai urutan input
     * @throws ChecksumIoException bila ada input yang bukan file biasa atau gagal dibaca
     */
    public List<FileChecksum> execute(
            List<Path> inputs, ChecksumAlgorithm algorithm, ProgressReporter progress, CancellationToken cancellation) {
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("At least one file is required");
        }
        // Folder yang di-drop ditolak di awal, sebelum file lain sempat dihitung
        inputs.stream().filter(input -> !files.isRegularFile(input)).findFirst().ifPresent(input -> {
            throw new ChecksumIoException(input.getFileName().toString(), null);
        });
        List<Long> sizes = inputs.stream().map(files::size).toList();
        long total = sizes.stream().mapToLong(Long::longValue).sum();
        ByteCounter counter = new ByteCounter(total, progress, cancellation);
        List<FileChecksum> results = new ArrayList<>(inputs.size());
        for (int index = 0; index < inputs.size(); index++) {
            cancellation.throwIfCancelled();
            Path input = inputs.get(index);
            long started = System.nanoTime();
            String checksum = files.checksum(input, algorithm, counter::add);
            Duration duration = Duration.ofNanos(System.nanoTime() - started);
            results.add(new FileChecksum(input.getFileName().toString(), sizes.get(index), checksum, duration));
        }
        return List.copyOf(results);
    }

    /** Akumulasi byte lintas file supaya progress bar mewakili seluruh pekerjaan, bukan per file. */
    private static final class ByteCounter {

        private final long total;
        private final ProgressReporter progress;
        private final CancellationToken cancellation;
        private long done;

        private ByteCounter(long total, ProgressReporter progress, CancellationToken cancellation) {
            this.total = total;
            this.progress = progress;
            this.cancellation = cancellation;
        }

        private void add(long bytes) {
            cancellation.throwIfCancelled();
            done += bytes;
            progress.report(done, total);
        }
    }
}
