package io.github.lukmnh.toolbox.json.viewer.application;

import io.github.lukmnh.toolbox.json.viewer.domain.RecentFiles;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Daftar file JSON terakhir dibuka: terbaru di depan, tanpa duplikat, maks 10. */
public final class RecentJsonFilesUseCase {

    private final RecentFiles recentFiles;

    public RecentJsonFilesUseCase(RecentFiles recentFiles) {
        this.recentFiles = recentFiles;
    }

    public List<Path> list() {
        return recentFiles.load();
    }

    /** @return daftar terbaru setelah {@code file} ditambahkan */
    public List<Path> remember(Path file) {
        Path normalized = file.toAbsolutePath().normalize();
        List<Path> updated = new ArrayList<>();
        updated.add(normalized);
        for (Path existing : recentFiles.load()) {
            if (updated.size() == RecentFiles.MAX_FILES) {
                break;
            }
            if (!existing.toAbsolutePath().normalize().equals(normalized)) {
                updated.add(existing);
            }
        }
        recentFiles.save(updated);
        return List.copyOf(updated);
    }
}
