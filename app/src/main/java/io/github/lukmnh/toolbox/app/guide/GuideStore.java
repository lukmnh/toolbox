package io.github.lukmnh.toolbox.app.guide;

import io.github.lukmnh.toolbox.common.io.AppDirectories;
import io.github.lukmnh.toolbox.common.io.AtomicFiles;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Guide Markdown: bawaan aplikasi ({@code /guide/guide.md}) atau hasil edit user di {@code ~/.toolbox/guide.md}.
 * Selama user belum mengedit, file user tidak ada sehingga update guide bawaan di versi baru langsung terlihat.
 */
public final class GuideStore {

    static final String DEFAULT_RESOURCE = "/guide/guide.md";
    private static final String FILE_NAME = "guide.md";

    private final Path file;

    public GuideStore(Path file) {
        this.file = file;
    }

    public static GuideStore inDataDirectory() {
        return new GuideStore(AppDirectories.dataDirectory().resolve(FILE_NAME));
    }

    /** Guide hasil edit user bila ada, selain itu guide bawaan. */
    public String load() throws IOException {
        return Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : defaultGuide();
    }

    public void save(String markdown) throws IOException {
        AtomicFiles.write(file, temp -> Files.writeString(temp, markdown, StandardCharsets.UTF_8));
    }

    /** Hapus hasil edit user lalu kembalikan guide bawaan. */
    public String reset() throws IOException {
        Files.deleteIfExists(file);
        return defaultGuide();
    }

    /** @throws UncheckedIOException bila resource bawaan hilang dari jar (kesalahan build) */
    public static String defaultGuide() {
        try (InputStream input = GuideStore.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input == null) {
                throw new IOException("Resource " + DEFAULT_RESOURCE + " not found");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the bundled guide", e);
        }
    }
}
