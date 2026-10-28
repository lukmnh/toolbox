package io.github.lukmnh.toolbox.devutils.checksum.application;

import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumFiles;
import io.github.lukmnh.toolbox.devutils.checksum.domain.FileChecksum;
import io.github.lukmnh.toolbox.devutils.checksum.domain.SumsFormat;
import java.nio.file.Path;
import java.util.List;

/** Tulis hasil Generate ke file format {@code sha256sum} (biasanya temp file sebelum di-download). */
public final class ExportSumsUseCase {

    private final ChecksumFiles files;

    public ExportSumsUseCase(ChecksumFiles files) {
        this.files = files;
    }

    public void execute(List<FileChecksum> checksums, Path target) {
        files.writeText(
                target,
                SumsFormat.write(
                        checksums.stream().map(FileChecksum::toSumsEntry).toList()));
    }
}
