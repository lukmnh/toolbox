package io.github.lukmnh.toolbox.devutils.encoder.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.devutils.encoder.domain.Base64Files;
import io.github.lukmnh.toolbox.devutils.encoder.domain.DataUri;
import io.github.lukmnh.toolbox.devutils.encoder.domain.EncodedFile;
import io.github.lukmnh.toolbox.devutils.encoder.domain.FileEncodeOptions;
import io.github.lukmnh.toolbox.devutils.encoder.domain.MimeTypes;
import java.nio.file.Path;

/** Encode satu file ke teks Base64 (opsional data URI) secara streaming. */
public final class EncodeFileUseCase {

    private final Base64Files files;

    public EncodeFileUseCase(Base64Files files) {
        this.files = files;
    }

    public EncodedFile execute(
            Path source,
            Path target,
            FileEncodeOptions options,
            ProgressReporter progress,
            CancellationToken cancellation) {
        String prefix = options.dataUri() ? DataUri.prefix(mimeType(source)) : "";
        return files.encode(source, target, options, prefix, (done, total) -> {
            cancellation.throwIfCancelled();
            progress.report(done, total);
        });
    }

    /** Deteksi OS lebih akurat; ekstensi hanya fallback (misalnya Windows tanpa asosiasi file). */
    private String mimeType(Path source) {
        return files.probeContentType(source)
                .orElseGet(() -> MimeTypes.forFileName(source.getFileName().toString()));
    }
}
