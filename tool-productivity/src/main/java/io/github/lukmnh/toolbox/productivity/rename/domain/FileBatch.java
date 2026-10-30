package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** File yang akan di-rename; semuanya berada langsung di {@code folder}. Urutan list = urutan penomoran. */
public record FileBatch(Path folder, List<FileEntry> files) {

    public FileBatch {
        Objects.requireNonNull(folder, "folder");
        files = List.copyOf(files);
    }
}
