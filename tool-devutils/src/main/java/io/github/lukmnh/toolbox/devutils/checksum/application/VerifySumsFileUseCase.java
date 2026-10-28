package io.github.lukmnh.toolbox.devutils.checksum.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumAlgorithm;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumFiles;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumIoException;
import io.github.lukmnh.toolbox.devutils.checksum.domain.SumsEntry;
import io.github.lukmnh.toolbox.devutils.checksum.domain.SumsFormat;
import io.github.lukmnh.toolbox.devutils.checksum.domain.SumsVerification;
import io.github.lukmnh.toolbox.devutils.checksum.domain.VerifyStatus;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Verifikasi semua file yang disebut di file daftar checksum ({@code SHA256SUMS}, {@code *.sha256}). Nama file
 * relatif terhadap folder file daftar; path absolut atau keluar folder berstatus {@link VerifyStatus#REJECTED} dan
 * tidak pernah dibaca (Security Requirements #6).
 */
public final class VerifySumsFileUseCase {

    private static final String NOT_COMPUTED = "";

    private final ChecksumFiles files;

    public VerifySumsFileUseCase(ChecksumFiles files) {
        this.files = files;
    }

    /** @param override algoritma pilihan user; kosong berarti dideteksi per entry dari panjang checksum */
    public List<SumsVerification> execute(
            Path sumsFile,
            Optional<ChecksumAlgorithm> override,
            ProgressReporter progress,
            CancellationToken cancellation) {
        List<SumsEntry> entries = SumsFormat.parse(files.readSumsText(sumsFile));
        Path directory = sumsFile.toAbsolutePath().getParent();
        List<SumsVerification> results = new ArrayList<>(entries.size());
        for (SumsEntry entry : entries) {
            cancellation.throwIfCancelled();
            results.add(verify(directory, entry, override, cancellation));
            progress.report(results.size(), entries.size());
        }
        return List.copyOf(results);
    }

    private SumsVerification verify(
            Path directory, SumsEntry entry, Optional<ChecksumAlgorithm> override, CancellationToken cancellation) {
        Optional<Path> resolved = files.resolveInside(directory, entry.fileName());
        if (resolved.isEmpty()) {
            return result(entry, VerifyStatus.REJECTED, NOT_COMPUTED);
        }
        if (!files.isRegularFile(resolved.get())) {
            return result(entry, VerifyStatus.NOT_FOUND, NOT_COMPUTED);
        }
        // SumsFormat sudah memastikan panjang checksum dikenali, jadi detect selalu berhasil
        ChecksumAlgorithm algorithm =
                override.or(() -> ChecksumAlgorithm.detect(entry.checksum())).orElseThrow();
        try {
            String actual = files.checksum(resolved.get(), algorithm, bytes -> cancellation.throwIfCancelled());
            return result(entry, actual.equals(entry.checksum()) ? VerifyStatus.OK : VerifyStatus.FAILED, actual);
        } catch (ChecksumIoException e) {
            // Satu file yang tidak bisa dibaca tidak boleh menggagalkan verifikasi file lain
            return result(entry, VerifyStatus.FAILED, NOT_COMPUTED);
        }
    }

    private static SumsVerification result(SumsEntry entry, VerifyStatus status, String actual) {
        return new SumsVerification(entry.fileName(), status, entry.checksum(), actual);
    }
}
