package io.github.lukmnh.toolbox.devutils.checksum.infrastructure;

import io.github.lukmnh.toolbox.common.security.SafePaths;
import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumAlgorithm;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumFiles;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumIoException;
import io.github.lukmnh.toolbox.devutils.checksum.domain.SumsFileTooLargeException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.LongConsumer;
import java.util.zip.CRC32;

/** Implementasi {@link ChecksumFiles} dengan JDK saja: {@link MessageDigest} dan {@link CRC32}. */
public final class JdkChecksumFiles implements ChecksumFiles {

    /** Buffer 1 MB: cukup besar untuk throughput disk, cukup kecil untuk progress yang halus. */
    private static final int BUFFER_BYTES = 1024 * 1024;

    private static final String CRC32_HEX_FORMAT = "%08x";

    @Override
    public long size(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new ChecksumIoException(name(file), e);
        }
    }

    @Override
    public boolean isRegularFile(Path file) {
        return Files.isRegularFile(file);
    }

    @Override
    public String checksum(Path file, ChecksumAlgorithm algorithm, LongConsumer bytesRead) {
        try (InputStream input = Files.newInputStream(file)) {
            return algorithm == ChecksumAlgorithm.CRC32 ? crc32(input, bytesRead) : digest(input, algorithm, bytesRead);
        } catch (IOException e) {
            throw new ChecksumIoException(name(file), e);
        }
    }

    @Override
    public Optional<Path> resolveInside(Path directory, String relativeName) {
        return SafePaths.resolveInside(directory, relativeName);
    }

    @Override
    public String readSumsText(Path file) {
        long size = size(file);
        if (size > SecurityLimits.CHECKSUM_LIST_MAX_FILE_BYTES) {
            throw new SumsFileTooLargeException(name(file), size, SecurityLimits.CHECKSUM_LIST_MAX_FILE_BYTES);
        }
        try {
            // Decode lenient: byte UTF-8 rusak jadi karakter pengganti, baris itu lalu gagal di parser dengan nomor
            // baris
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ChecksumIoException(name(file), e);
        }
    }

    @Override
    public void writeText(Path file, String text) {
        try {
            Files.writeString(file, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ChecksumIoException(name(file), e);
        }
    }

    private static String digest(InputStream input, ChecksumAlgorithm algorithm, LongConsumer bytesRead)
            throws IOException {
        MessageDigest digest = messageDigest(algorithm);
        byte[] buffer = new byte[BUFFER_BYTES];
        int read;
        while ((read = input.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
            bytesRead.accept(read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String crc32(InputStream input, LongConsumer bytesRead) throws IOException {
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[BUFFER_BYTES];
        int read;
        while ((read = input.read(buffer)) != -1) {
            crc.update(buffer, 0, read);
            bytesRead.accept(read);
        }
        return String.format(CRC32_HEX_FORMAT, crc.getValue());
    }

    private static MessageDigest messageDigest(ChecksumAlgorithm algorithm) {
        try {
            return MessageDigest.getInstance(algorithm.jcaName());
        } catch (NoSuchAlgorithmException e) {
            // SHA-1/256/512 dan MD5 wajib tersedia di setiap JDK; sampai sini berarti runtime rusak
            throw new IllegalStateException("Algorithm not available: " + algorithm.label(), e);
        }
    }

    private static String name(Path file) {
        Path fileName = file.getFileName();
        return fileName == null ? file.toString() : fileName.toString();
    }
}
