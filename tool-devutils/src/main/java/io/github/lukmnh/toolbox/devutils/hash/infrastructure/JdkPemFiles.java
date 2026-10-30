package io.github.lukmnh.toolbox.devutils.hash.infrastructure;

import io.github.lukmnh.toolbox.devutils.hash.domain.KeyExportException;
import io.github.lukmnh.toolbox.devutils.hash.domain.PemFiles;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Tulis PEM (ASCII) ke file. Target selalu temp file di folder owner-only milik aplikasi. */
public final class JdkPemFiles implements PemFiles {

    @Override
    public void write(Path target, String pem) {
        try {
            Files.writeString(target, pem, StandardCharsets.US_ASCII);
        } catch (IOException e) {
            throw new KeyExportException(target.getFileName().toString(), e);
        }
    }
}
