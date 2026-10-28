package io.github.lukmnh.toolbox.devutils.checksum.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.common.task.TaskCancelledException;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumAlgorithm;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumIoException;
import io.github.lukmnh.toolbox.devutils.checksum.domain.FileChecksum;
import io.github.lukmnh.toolbox.devutils.checksum.domain.FileVerification;
import io.github.lukmnh.toolbox.devutils.checksum.domain.InvalidChecksumException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GenerateAndVerifyFileUseCaseTest {

    private static final Path A = Path.of("a.txt").toAbsolutePath();
    private static final Path B = Path.of("dir/b.txt").toAbsolutePath();
    private static final String SHA256 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
    private static final String CRC32 = "352441c2";

    @Test
    void generateReportsProgressAcrossAllFilesAndKeepsOrder() {
        FakeChecksumFiles files = new FakeChecksumFiles().add(A, SHA256, 25).add(B, CRC32, 15);
        List<Long> progress = new ArrayList<>();

        List<FileChecksum> results = new GenerateChecksumsUseCase(files)
                .execute(
                        List.of(A, B),
                        ChecksumAlgorithm.SHA_256,
                        (done, total) -> {
                            assertEquals(40, total);
                            progress.add(done);
                        },
                        CancellationToken.none());

        assertEquals(
                List.of("a.txt", "b.txt"),
                results.stream().map(FileChecksum::fileName).toList());
        assertEquals(25, results.get(0).sizeBytes());
        assertEquals(40L, progress.getLast());
    }

    @Test
    void generateRejectsDirectoryOrMissingFileBeforeReading() {
        FakeChecksumFiles files = new FakeChecksumFiles().add(A, SHA256, 1);
        assertThrows(
                ChecksumIoException.class,
                () -> new GenerateChecksumsUseCase(files)
                        .execute(
                                List.of(A, B),
                                ChecksumAlgorithm.SHA_256,
                                ProgressReporter.NONE,
                                CancellationToken.none()));
        assertTrue(files.readFiles().isEmpty());
    }

    @Test
    void generateIsCancellableMidFile() {
        FakeChecksumFiles files = new FakeChecksumFiles().add(A, SHA256, 100);
        CancellationToken token = new CancellationToken();
        assertThrows(
                TaskCancelledException.class,
                () -> new GenerateChecksumsUseCase(files)
                        .execute(List.of(A), ChecksumAlgorithm.SHA_256, (done, total) -> token.cancel(), token));
    }

    @Test
    void exportWritesSha256sumFormat() {
        FakeChecksumFiles files = new FakeChecksumFiles();
        Path target = Path.of("out.txt");
        new ExportSumsUseCase(files).execute(List.of(new FileChecksum("a.txt", 3, SHA256, Duration.ZERO)), target);
        assertEquals(SHA256 + "  a.txt\n", files.written(target));
    }

    @Test
    void verifyMatchesTrimmedCaseInsensitiveChecksumWithAutoDetect() {
        FakeChecksumFiles files = new FakeChecksumFiles().add(A, CRC32, 3);
        FileVerification result = new VerifyFileUseCase(files)
                .execute(A, "  352441C2\n", Optional.empty(), ProgressReporter.NONE, CancellationToken.none());
        assertTrue(result.matches());
        assertEquals(ChecksumAlgorithm.CRC32, result.algorithm());
    }

    @Test
    void verifyReportsMismatch() {
        FakeChecksumFiles files = new FakeChecksumFiles().add(A, SHA256, 3);
        FileVerification result = new VerifyFileUseCase(files)
                .execute(A, "0".repeat(64), Optional.empty(), ProgressReporter.NONE, CancellationToken.none());
        assertFalse(result.matches());
        assertEquals(SHA256, result.actual());
    }

    @Test
    void verifyUsesOverrideAlgorithm() {
        FakeChecksumFiles files = new FakeChecksumFiles().add(A, SHA256, 3);
        FileVerification result = new VerifyFileUseCase(files)
                .execute(
                        A,
                        SHA256,
                        Optional.of(ChecksumAlgorithm.SHA_512),
                        ProgressReporter.NONE,
                        CancellationToken.none());
        assertEquals(ChecksumAlgorithm.SHA_512, result.algorithm());
    }

    @Test
    void verifyRejectsInvalidChecksumAndMissingFile() {
        FakeChecksumFiles files = new FakeChecksumFiles().add(A, SHA256, 3);
        VerifyFileUseCase useCase = new VerifyFileUseCase(files);
        assertThrows(
                InvalidChecksumException.class,
                () -> useCase.execute(A, "xyz", Optional.empty(), ProgressReporter.NONE, CancellationToken.none()));
        assertThrows(
                InvalidChecksumException.class,
                () -> useCase.execute(A, "abcd", Optional.empty(), ProgressReporter.NONE, CancellationToken.none()));
        assertThrows(
                ChecksumIoException.class,
                () -> useCase.execute(B, SHA256, Optional.empty(), ProgressReporter.NONE, CancellationToken.none()));
    }
}
