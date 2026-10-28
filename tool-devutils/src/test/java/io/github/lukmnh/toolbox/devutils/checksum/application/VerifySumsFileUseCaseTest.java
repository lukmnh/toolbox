package io.github.lukmnh.toolbox.devutils.checksum.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.common.task.TaskCancelledException;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumAlgorithm;
import io.github.lukmnh.toolbox.devutils.checksum.domain.SumsVerification;
import io.github.lukmnh.toolbox.devutils.checksum.domain.VerifyStatus;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VerifySumsFileUseCaseTest {

    private static final Path DIRECTORY = Path.of("/data/release").toAbsolutePath();
    private static final Path SUMS = DIRECTORY.resolve("SHA256SUMS");
    private static final String GOOD = "a".repeat(64);
    private static final String BAD = "b".repeat(64);

    @Test
    void reportsOkFailedNotFoundAndRejected() {
        String sums = GOOD + "  app.jar\n"
                + GOOD + "  lib/core.jar\n"
                + GOOD + "  missing.jar\n"
                + GOOD + "  ../../etc/passwd\n"
                + GOOD + "  /etc/shadow\n";
        FakeChecksumFiles files = new FakeChecksumFiles()
                .addText(SUMS, sums)
                .add(DIRECTORY.resolve("app.jar"), GOOD, 25)
                .add(DIRECTORY.resolve("lib/core.jar"), BAD, 5);

        List<SumsVerification> results = new VerifySumsFileUseCase(files)
                .execute(SUMS, Optional.empty(), ProgressReporter.NONE, CancellationToken.none());

        assertEquals(
                List.of(
                        VerifyStatus.OK,
                        VerifyStatus.FAILED,
                        VerifyStatus.NOT_FOUND,
                        VerifyStatus.REJECTED,
                        VerifyStatus.REJECTED),
                results.stream().map(SumsVerification::status).toList());
        assertEquals(BAD, results.get(1).actual());
        assertEquals("", results.get(3).actual());
    }

    @Test
    void rejectedEntriesAreNeverRead() {
        String sums = GOOD + "  ../../etc/passwd\n" + GOOD + "  /etc/passwd\n";
        FakeChecksumFiles files = new FakeChecksumFiles()
                .addText(SUMS, sums)
                .add(Path.of("/etc/passwd"), GOOD, 1)
                .add(Path.of("/data/etc/passwd"), GOOD, 1);

        new VerifySumsFileUseCase(files)
                .execute(SUMS, Optional.empty(), ProgressReporter.NONE, CancellationToken.none());

        assertEquals(List.of(), files.readFiles());
    }

    @Test
    void unreadableFileFailsOnlyThatEntry() {
        FakeChecksumFiles files = new FakeChecksumFiles()
                .addText(SUMS, GOOD + "  a.bin\n" + GOOD + "  b.bin\n")
                .add(DIRECTORY.resolve("a.bin"), GOOD, 1)
                .add(DIRECTORY.resolve("b.bin"), GOOD, 1)
                .unreadable(DIRECTORY.resolve("a.bin"));

        List<SumsVerification> results = new VerifySumsFileUseCase(files)
                .execute(SUMS, Optional.empty(), ProgressReporter.NONE, CancellationToken.none());

        assertEquals(VerifyStatus.FAILED, results.get(0).status());
        assertEquals(VerifyStatus.OK, results.get(1).status());
    }

    @Test
    void overrideAlgorithmIsUsedAndProgressCountsEntries() {
        FakeChecksumFiles files =
                new FakeChecksumFiles().addText(SUMS, GOOD + "  a.bin\n").add(DIRECTORY.resolve("a.bin"), GOOD, 1);
        long[] last = new long[2];

        new VerifySumsFileUseCase(files)
                .execute(
                        SUMS,
                        Optional.of(ChecksumAlgorithm.SHA_256),
                        (done, total) -> {
                            last[0] = done;
                            last[1] = total;
                        },
                        CancellationToken.none());

        assertEquals(1, last[0]);
        assertEquals(1, last[1]);
    }

    @Test
    void cancellationStopsBeforeReadingMoreFiles() {
        FakeChecksumFiles files =
                new FakeChecksumFiles().addText(SUMS, GOOD + "  a.bin\n").add(DIRECTORY.resolve("a.bin"), GOOD, 1);
        CancellationToken token = new CancellationToken();
        token.cancel();

        assertThrows(
                TaskCancelledException.class,
                () -> new VerifySumsFileUseCase(files).execute(SUMS, Optional.empty(), ProgressReporter.NONE, token));
        assertFalse(files.readFiles().contains(DIRECTORY.resolve("a.bin")));
    }
}
