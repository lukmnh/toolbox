package io.github.lukmnh.toolbox.devutils.encoder.application;

import io.github.lukmnh.toolbox.devutils.encoder.domain.Base64Files;
import io.github.lukmnh.toolbox.devutils.encoder.domain.DecodedFile;
import io.github.lukmnh.toolbox.devutils.encoder.domain.EncodedFile;
import io.github.lukmnh.toolbox.devutils.encoder.domain.FileEncodeOptions;
import io.github.lukmnh.toolbox.devutils.encoder.domain.FileType;
import io.github.lukmnh.toolbox.devutils.encoder.domain.ImageSize;
import io.github.lukmnh.toolbox.devutils.encoder.domain.ProgressListener;
import io.github.lukmnh.toolbox.devutils.encoder.domain.TextPreview;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Port in-memory: mencatat argumen dan menyimpan isi file sebagai byte. */
final class FakeBase64Files implements Base64Files {

    final Map<Path, byte[]> written = new HashMap<>();
    Optional<String> probedType = Optional.empty();
    Optional<ImageSize> imageSize = Optional.empty();
    String lastPrefix;
    long fileSize = 100;
    int progressSteps = 3;

    @Override
    public long size(Path file) {
        return fileSize;
    }

    @Override
    public EncodedFile encode(
            Path source, Path target, FileEncodeOptions options, String dataUriPrefix, ProgressListener progress) {
        lastPrefix = dataUriPrefix;
        for (int step = 1; step <= progressSteps; step++) {
            progress.advanced(step, progressSteps);
        }
        return new EncodedFile(target, fileSize, fileSize * 4 / 3);
    }

    @Override
    public DecodedFile decode(Path source, Path target, ProgressListener progress) {
        progress.advanced(fileSize, fileSize);
        return new DecodedFile(target, fileSize, FileType.BINARY);
    }

    @Override
    public void writeBytes(Path target, byte[] content) {
        written.put(target, content.clone());
    }

    @Override
    public TextPreview readPreview(Path file, int maxBytes) {
        return new TextPreview("preview", false);
    }

    @Override
    public String readText(Path file) {
        return "full";
    }

    @Override
    public Optional<String> probeContentType(Path file) {
        return probedType;
    }

    @Override
    public Optional<ImageSize> imageSize(Path file) {
        return imageSize;
    }
}
