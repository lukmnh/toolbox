package io.github.lukmnh.toolbox.devutils.checksum.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.devutils.checksum.application.VerifySumsFileUseCase;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumAlgorithm;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumIoException;
import io.github.lukmnh.toolbox.devutils.checksum.domain.SumsFileTooLargeException;
import io.github.lukmnh.toolbox.devutils.checksum.domain.SumsVerification;
import io.github.lukmnh.toolbox.devutils.checksum.domain.VerifyStatus;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JdkChecksumFilesTest {

    /** Nilai referensi dari {@code sha256sum}, {@code md5sum}, dst. untuk isi "abc". */
    private static final Map<ChecksumAlgorithm, String> ABC = Map.of(
            ChecksumAlgorithm.SHA_256, "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            ChecksumAlgorithm.SHA_1, "a9993e364706816aba3e25717850c26c9cd0d89d",
            ChecksumAlgorithm.SHA_512,
                    "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
            ChecksumAlgorithm.MD5, "900150983cd24fb0d6963f7d28e17f72",
            ChecksumAlgorithm.CRC32, "352441c2");

    private final JdkChecksumFiles files = new JdkChecksumFiles();

    @TempDir
    Path directory;

    @Test
    void checksumMatchesReferenceValuesForEveryAlgorithm() throws IOException {
        Path file = Files.writeString(directory.resolve("abc.txt"), "abc");
        for (ChecksumAlgorithm algorithm : ChecksumAlgorithm.values()) {
            assertEquals(ABC.get(algorithm), files.checksum(file, algorithm, bytes -> {}), algorithm.label());
        }
    }

    @Test
    void progressCoversWholeFileAcrossBuffers() throws IOException {
        Path file = directory.resolve("big.bin");
        long size = 3L * 1024 * 1024 + 7;
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
            raf.setLength(size);
        }
        AtomicLong read = new AtomicLong();
        files.checksum(file, ChecksumAlgorithm.CRC32, read::addAndGet);
        assertEquals(size, read.get());
    }

    @Test
    void sumsFileWithTraversalIsVerifiedWithoutLeavingFolder(@TempDir Path outside) throws IOException {
        Files.writeString(outside.resolve("secret.txt"), "abc");
        Files.writeString(directory.resolve("abc.txt"), "abc");
        String sha = ABC.get(ChecksumAlgorithm.SHA_256);
        Path sums = Files.writeString(
                directory.resolve("SHA256SUMS"),
                sha + "  abc.txt\n"
                        + sha + "  ../" + outside.getFileName() + "/secret.txt\n"
                        + sha + "  " + outside.resolve("secret.txt") + "\n"
                        + sha + "  ../../etc/passwd\n"
                        + sha + "  hilang.txt\n");

        List<SumsVerification> results = new VerifySumsFileUseCase(files)
                .execute(sums, Optional.empty(), ProgressReporter.NONE, CancellationToken.none());

        assertEquals(
                List.of(
                        VerifyStatus.OK,
                        VerifyStatus.REJECTED,
                        VerifyStatus.REJECTED,
                        VerifyStatus.REJECTED,
                        VerifyStatus.NOT_FOUND),
                results.stream().map(SumsVerification::status).toList());
    }

    @Test
    void oversizedSumsFileIsRejectedBeforeReading() throws IOException {
        Path sums = directory.resolve("SHA256SUMS");
        try (RandomAccessFile raf = new RandomAccessFile(sums.toFile(), "rw")) {
            raf.setLength(SecurityLimits.CHECKSUM_LIST_MAX_FILE_BYTES + 1);
        }
        assertThrows(SumsFileTooLargeException.class, () -> files.readSumsText(sums));
    }

    @Test
    void missingFileBecomesDomainErrorWithFileNameOnly() {
        ChecksumIoException error = assertThrows(
                ChecksumIoException.class,
                () -> files.checksum(directory.resolve("tidak-ada.bin"), ChecksumAlgorithm.MD5, bytes -> {}));
        assertEquals("tidak-ada.bin", error.fileName());
    }

    @Test
    void writeTextThenReadBack() throws IOException {
        Path target = directory.resolve("out.txt");
        files.writeText(target, "isi\n");
        assertEquals("isi\n", files.readSumsText(target));
        assertTrue(files.isRegularFile(target));
    }
}
