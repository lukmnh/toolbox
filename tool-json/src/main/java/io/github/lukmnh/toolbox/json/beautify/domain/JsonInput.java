package io.github.lukmnh.toolbox.json.beautify.domain;

import io.github.lukmnh.toolbox.json.shared.domain.NotJsonFileException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/** Sumber JSON Beautify: teks yang di-paste atau file {@code .json}. */
public sealed interface JsonInput {

    long sizeBytes();

    record Text(String content) implements JsonInput {

        public Text {
            Objects.requireNonNull(content, "content");
        }

        /** Ukuran dalam UTF-8, dihitung tanpa membuat salinan byte. */
        @Override
        public long sizeBytes() {
            long bytes = 0;
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c < 0x80) {
                    bytes += 1;
                } else if (c < 0x800) {
                    bytes += 2;
                } else if (Character.isHighSurrogate(c) && i + 1 < content.length()) {
                    bytes += 4;
                    i++;
                } else {
                    bytes += 3;
                }
            }
            return bytes;
        }
    }

    /** File {@code .json} (case-insensitive). Ekstensi lain ditolak dengan {@link NotJsonFileException}. */
    record FileInput(Path file, long sizeBytes) implements JsonInput {

        private static final String EXTENSION = ".json";

        public FileInput {
            Objects.requireNonNull(file, "file");
            if (!isJsonFile(file)) {
                throw new NotJsonFileException(file);
            }
        }

        public static boolean isJsonFile(Path file) {
            Path name = file.getFileName();
            return name != null && name.toString().toLowerCase(Locale.ROOT).endsWith(EXTENSION);
        }

        public String baseName() {
            String name = file.getFileName().toString();
            return name.substring(0, name.length() - EXTENSION.length());
        }
    }
}
