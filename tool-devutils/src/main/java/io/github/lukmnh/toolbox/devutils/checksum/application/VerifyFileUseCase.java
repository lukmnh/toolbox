package io.github.lukmnh.toolbox.devutils.checksum.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumAlgorithm;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumFiles;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumIoException;
import io.github.lukmnh.toolbox.devutils.checksum.domain.FileVerification;
import io.github.lukmnh.toolbox.devutils.checksum.domain.InvalidChecksumException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** Bandingkan checksum satu file dengan checksum yang ditempel user (trim, case-insensitive). */
public final class VerifyFileUseCase {

    private final ChecksumFiles files;

    public VerifyFileUseCase(ChecksumFiles files) {
        this.files = files;
    }

    /**
     * @param override algoritma pilihan user; kosong berarti auto-detect dari panjang checksum
     * @throws InvalidChecksumException bila checksum bukan hex atau panjangnya tidak dikenali
     * @throws ChecksumIoException bila file tidak ada, berupa folder, atau gagal dibaca
     */
    public FileVerification execute(
            Path file,
            String expectedChecksum,
            Optional<ChecksumAlgorithm> override,
            ProgressReporter progress,
            CancellationToken cancellation) {
        String expected = ChecksumAlgorithm.normalize(expectedChecksum);
        if (!ChecksumAlgorithm.isHex(expected)) {
            throw new InvalidChecksumException();
        }
        ChecksumAlgorithm algorithm =
                override.or(() -> ChecksumAlgorithm.detect(expected)).orElseThrow(InvalidChecksumException::new);
        if (!files.isRegularFile(file)) {
            throw new ChecksumIoException(file.getFileName().toString(), null);
        }
        long total = files.size(file);
        AtomicLong done = new AtomicLong();
        String actual = files.checksum(file, algorithm, bytes -> {
            cancellation.throwIfCancelled();
            progress.report(done.addAndGet(bytes), total);
        });
        return new FileVerification(file.getFileName().toString(), algorithm, expected, actual);
    }
}
