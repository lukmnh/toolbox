package io.github.lukmnh.toolbox.devutils;

import io.github.lukmnh.toolbox.common.security.CsvFormulaGuard;
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
import io.github.lukmnh.toolbox.devutils.cron.application.BuildCronUseCase;
import io.github.lukmnh.toolbox.devutils.cron.application.ExplainCronUseCase;
import io.github.lukmnh.toolbox.devutils.cron.infrastructure.CronUtilsEngine;
import io.github.lukmnh.toolbox.devutils.cron.presentation.CronTool;
import io.github.lukmnh.toolbox.devutils.cron.presentation.CronUseCases;
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
import io.github.lukmnh.toolbox.devutils.git.application.BuildGitCommandUseCase;
import io.github.lukmnh.toolbox.devutils.git.presentation.GitTool;
import io.github.lukmnh.toolbox.devutils.hash.application.ExportPemUseCase;
import io.github.lukmnh.toolbox.devutils.hash.application.GenerateKeyPairUseCase;
import io.github.lukmnh.toolbox.devutils.hash.application.GenerateSecretUseCase;
import io.github.lukmnh.toolbox.devutils.hash.application.GenerateUuidsUseCase;
import io.github.lukmnh.toolbox.devutils.hash.application.HashTextUseCase;
import io.github.lukmnh.toolbox.devutils.hash.application.HmacTextUseCase;
import io.github.lukmnh.toolbox.devutils.hash.domain.TextHasher;
import io.github.lukmnh.toolbox.devutils.hash.domain.UuidV7Generator;
import io.github.lukmnh.toolbox.devutils.hash.infrastructure.JdkPemFiles;
import io.github.lukmnh.toolbox.devutils.hash.infrastructure.JdkTextHasher;
import io.github.lukmnh.toolbox.devutils.hash.presentation.HashTool;
import io.github.lukmnh.toolbox.devutils.hash.presentation.HashUseCases;
import io.github.lukmnh.toolbox.devutils.jwt.application.DecodeJwtUseCase;
import io.github.lukmnh.toolbox.devutils.jwt.application.VerifyJwtSignatureUseCase;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JwtSignatureVerifier;
import io.github.lukmnh.toolbox.devutils.jwt.infrastructure.JacksonJwtJson;
import io.github.lukmnh.toolbox.devutils.jwt.infrastructure.JdkPublicKeyParser;
import io.github.lukmnh.toolbox.devutils.jwt.presentation.JwtTool;
import io.github.lukmnh.toolbox.devutils.mock.application.ExportMockUseCase;
import io.github.lukmnh.toolbox.devutils.mock.application.PreviewMockUseCase;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockRenderer;
import io.github.lukmnh.toolbox.devutils.mock.infrastructure.JdkMockFiles;
import io.github.lukmnh.toolbox.devutils.mock.presentation.MockTool;
import io.github.lukmnh.toolbox.devutils.mock.presentation.MockUseCases;
import io.github.lukmnh.toolbox.devutils.qr.application.ExportQrPngUseCase;
import io.github.lukmnh.toolbox.devutils.qr.application.GenerateQrUseCase;
import io.github.lukmnh.toolbox.devutils.qr.infrastructure.ImageIoQrPngWriter;
import io.github.lukmnh.toolbox.devutils.qr.infrastructure.ZxingQrEncoder;
import io.github.lukmnh.toolbox.devutils.qr.presentation.QrTool;
import io.github.lukmnh.toolbox.devutils.qr.presentation.QrUseCases;
import io.github.lukmnh.toolbox.devutils.regex.application.AnalyzeRegexUseCase;
import io.github.lukmnh.toolbox.devutils.regex.application.LoadTestTextUseCase;
import io.github.lukmnh.toolbox.devutils.regex.application.RegexCompiler;
import io.github.lukmnh.toolbox.devutils.regex.application.ReplaceRegexUseCase;
import io.github.lukmnh.toolbox.devutils.regex.application.RunRegexUseCase;
import io.github.lukmnh.toolbox.devutils.regex.infrastructure.JdkRegexEngine;
import io.github.lukmnh.toolbox.devutils.regex.infrastructure.JdkTestTextFiles;
import io.github.lukmnh.toolbox.devutils.regex.infrastructure.Re2jRegexEngine;
import io.github.lukmnh.toolbox.devutils.regex.presentation.RegexTool;
import io.github.lukmnh.toolbox.devutils.regex.presentation.RegexUseCases;
import io.github.lukmnh.toolbox.devutils.sql.application.ExportSqlUseCase;
import io.github.lukmnh.toolbox.devutils.sql.application.FormatSqlUseCase;
import io.github.lukmnh.toolbox.devutils.sql.application.LoadSqlFileUseCase;
import io.github.lukmnh.toolbox.devutils.sql.application.MinifySqlUseCase;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlFiles;
import io.github.lukmnh.toolbox.devutils.sql.infrastructure.JdkSqlFiles;
import io.github.lukmnh.toolbox.devutils.sql.infrastructure.VerticalBlankSqlFormatter;
import io.github.lukmnh.toolbox.devutils.sql.presentation.SqlTool;
import io.github.lukmnh.toolbox.devutils.sql.presentation.SqlUseCases;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;

/**
 * Composition root module Dev Utils: merakit infrastructure → use case → tool secara manual (tanpa DI framework).
 * Didaftarkan di {@code META-INF/services/io.github.lukmnh.toolbox.common.tool.ToolProvider}.
 */
public final class DevUtilsToolsProvider implements ToolProvider {

    @Override
    public List<Tool> tools() {
        return List.of(
                jwtTool(),
                encoderTool(),
                hashTool(),
                checksumTool(),
                diffTool(),
                regexTool(),
                sqlTool(),
                cronTool(),
                qrTool(),
                new GitTool(new BuildGitCommandUseCase()),
                mockTool());
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
        SecureRandom random = new SecureRandom();
        return new HashTool(new HashUseCases(
                new HashTextUseCase(hasher),
                new HmacTextUseCase(hasher),
                new GenerateSecretUseCase(random),
                new GenerateKeyPairUseCase(random),
                new GenerateUuidsUseCase(new UuidV7Generator(System::currentTimeMillis, random)),
                new ExportPemUseCase(new JdkPemFiles())));
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

    private static RegexTool regexTool() {
        RegexCompiler compiler = new RegexCompiler(List.of(new JdkRegexEngine(), new Re2jRegexEngine()));
        return new RegexTool(new RegexUseCases(
                new AnalyzeRegexUseCase(),
                new RunRegexUseCase(compiler),
                new ReplaceRegexUseCase(compiler),
                new LoadTestTextUseCase(new JdkTestTextFiles())));
    }

    private static SqlTool sqlTool() {
        SqlFiles files = new JdkSqlFiles();
        return new SqlTool(new SqlUseCases(
                new LoadSqlFileUseCase(files),
                new FormatSqlUseCase(new VerticalBlankSqlFormatter()),
                new MinifySqlUseCase(),
                new ExportSqlUseCase(files)));
    }

    private static CronTool cronTool() {
        return new CronTool(new CronUseCases(
                new ExplainCronUseCase(new CronUtilsEngine(), Clock.systemUTC()), new BuildCronUseCase()));
    }

    private static MockTool mockTool() {
        // CsvFormulaGuard disuntik di sini: domain hanya boleh JDK (ArchitectureTest)
        MockRenderer renderer = new MockRenderer(CsvFormulaGuard::neutralize);
        return new MockTool(new MockUseCases(
                new PreviewMockUseCase(renderer), new ExportMockUseCase(renderer, new JdkMockFiles())));
    }

    private static QrTool qrTool() {
        return new QrTool(new QrUseCases(
                new GenerateQrUseCase(new ZxingQrEncoder()), new ExportQrPngUseCase(new ImageIoQrPngWriter())));
    }
}
