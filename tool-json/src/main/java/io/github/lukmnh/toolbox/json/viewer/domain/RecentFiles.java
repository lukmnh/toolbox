package io.github.lukmnh.toolbox.json.viewer.domain;

import java.nio.file.Path;
import java.util.List;

/** Port: daftar file JSON terakhir dibuka (maks {@link #MAX_FILES}, terbaru di depan). */
public interface RecentFiles {

    int MAX_FILES = 10;

    List<Path> load();

    void save(List<Path> files);
}
