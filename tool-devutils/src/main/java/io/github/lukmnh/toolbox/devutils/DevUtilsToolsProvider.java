package io.github.lukmnh.toolbox.devutils;

import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolProvider;
import io.github.lukmnh.toolbox.devutils.checksum.application.ExportSumsUseCase;
import io.github.lukmnh.toolbox.devutils.checksum.application.GenerateChecksumsUseCase;
import io.github.lukmnh.toolbox.devutils.checksum.application.VerifyFileUseCase;
import io.github.lukmnh.toolbox.devutils.checksum.application.VerifySumsFileUseCase;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumFiles;
import io.github.lukmnh.toolbox.devutils.checksum.infrastructure.JdkChecksumFiles;
import io.github.lukmnh.toolbox.devutils.checksum.presentation.ChecksumTool;
import io.github.lukmnh.toolbox.devutils.checksum.presentation.ChecksumUseCases;
import io.github.lukmnh.toolbox.devutils.diff.application.CompareTextsUseCase;
import io.github.lukmnh.toolbox.devutils.diff.application.ExportUnifiedDiffUseCase;
import io.github.lukmnh.toolbox.devutils.diff.application.LoadTextFileUseCase;
import io.github.lukmnh.toolbox.devutils.diff.domain.TextFiles;
import io.github.lukmnh.toolbox.devutils.diff.infrastructure.JavaDiffUtilsDiffer;
import io.github.lukmnh.toolbox.devutils.diff.infrastructure.JdkTextFiles;
import io.github.lukmnh.toolbox.devutils.diff.presentation.DiffTool;
import io.github.lukmnh.toolbox.devutils.diff.presentation.DiffUseCases;
import io.github.lukmnh.toolbox.devutils.encoder.application.DecodeToFileUseCase;
import io.github.lukmnh.toolbox.devutils.encoder.application.EncodeFileUseCase;
import io.github.lukmnh.toolbox.devutils.encoder.application.ReadFileUseCase;
import io.github.lukmnh.toolbox.devutils.encoder.domain.Base64Files;
import io.github.lukmnh.toolbox.devutils.encoder.infrastructure.JdkBase64Files;
import io.github.lukmnh.toolbox.devutils.encoder.presentation.EncoderTool;
import io.github.lukmnh.toolbox.devutils.encoder.presentation.EncoderUseCases;
import io.github.lukmnh.toolbox.devutils.hash.application.HashTextUseCase;
import io.github.lukmnh.toolbox.devutils.hash.application.HmacTextUseCase;
import io.github.lukmnh.toolbox.devutils.hash.domain.TextHasher;
import io.github.lukmnh.toolbox.devutils.hash.infrastructure.JdkTextHasher;
import io.github.lukmnh.toolbox.devutils.hash.presentation.HashTool;
import io.github.lukmnh.toolbox.devutils.jwt.application.DecodeJwtUseCase;
import io.github.lukmnh.toolbox.devutils.jwt.application.VerifyJwtSignatureUseCase;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JwtSignatureVerifier;
import io.github.lukmnh.toolbox.devutils.jwt.infrastructure.JacksonJwtJson;
import io.github.lukmnh.toolbox.devutils.jwt.infrastructure.JdkPublicKeyParser;
import io.github.lukmnh.toolbox.devutils.jwt.presentation.JwtTool;
import java.util.List;

/**
 * Composition root module Dev Utils: merakit infrastructure → use case → tool secara manual (tanpa DI framework).
 * Didaftarkan di {@code META-INF/services/io.github.lukmnh.toolbox.common.tool.ToolProvider}.
 */
public final class DevUtilsToolsProvider implements ToolProvider {

    @Override
    public List<Tool> tools() {
        return List.of(jwtTool(), encoderTool(), hashTool(), checksumTool(), diffTool());
    }

    private static JwtTool jwtTool() {
        return new JwtTool(
                new DecodeJwtUseCase(new JacksonJwtJson()),
                new VerifyJwtSignatureUseCase(new JdkPublicKeyParser(), new JwtSignatureVerifier()));
    }

    private static EncoderTool encoderTool() {
        Base64Files files = new JdkBase64Files();
        return new EncoderTool(new EncoderUseCases(
                new EncodeFileUseCase(files), new DecodeToFileUseCase(files), new ReadFileUseCase(files)));
    }

    private static HashTool hashTool() {
        TextHasher hasher = new JdkTextHasher();
        return new HashTool(new HashTextUseCase(hasher), new HmacTextUseCase(hasher));
    }

    private static ChecksumTool checksumTool() {
        ChecksumFiles files = new JdkChecksumFiles();
        return new ChecksumTool(new ChecksumUseCases(
                new GenerateChecksumsUseCase(files),
                new ExportSumsUseCase(files),
                new VerifyFileUseCase(files),
                new VerifySumsFileUseCase(files)));
    }

    private static DiffTool diffTool() {
        TextFiles files = new JdkTextFiles();
        return new DiffTool(new DiffUseCases(
                new LoadTextFileUseCase(files),
                new CompareTextsUseCase(new JavaDiffUtilsDiffer()),
                new ExportUnifiedDiffUseCase(files)));
    }
}
