package io.github.lukmnh.toolbox.devutils.checksum.presentation;

import io.github.lukmnh.toolbox.devutils.checksum.application.ExportSumsUseCase;
import io.github.lukmnh.toolbox.devutils.checksum.application.GenerateChecksumsUseCase;
import io.github.lukmnh.toolbox.devutils.checksum.application.VerifyFileUseCase;
import io.github.lukmnh.toolbox.devutils.checksum.application.VerifySumsFileUseCase;
import java.util.Objects;

/** Kumpulan use case Checksum, supaya constructor tool dan ViewModel tidak terlalu panjang. */
public record ChecksumUseCases(
        GenerateChecksumsUseCase generate,
        ExportSumsUseCase export,
        VerifyFileUseCase verifyFile,
        VerifySumsFileUseCase verifySums) {

    public ChecksumUseCases {
        Objects.requireNonNull(generate, "generate");
        Objects.requireNonNull(export, "export");
        Objects.requireNonNull(verifyFile, "verifyFile");
        Objects.requireNonNull(verifySums, "verifySums");
    }
}
