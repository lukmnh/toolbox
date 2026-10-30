package io.github.lukmnh.toolbox.devutils.mock.infrastructure;

import io.github.lukmnh.toolbox.devutils.mock.domain.MockFiles;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockIoException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Tulis output mock sebagai UTF-8 lewat BufferedWriter (streaming, tidak menampung semua baris di memori). */
public final class JdkMockFiles implements MockFiles {

    @Override
    public void write(Path target, MockContent content) {
        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            content.writeTo(writer);
        } catch (IOException e) {
            throw new MockIoException(target.getFileName().toString(), e);
        }
    }
}
