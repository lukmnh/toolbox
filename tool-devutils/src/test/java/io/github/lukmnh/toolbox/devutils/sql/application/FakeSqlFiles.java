package io.github.lukmnh.toolbox.devutils.sql.application;

import io.github.lukmnh.toolbox.devutils.sql.domain.LoadedSql;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlFiles;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Fake {@link SqlFiles} in-memory. */
final class FakeSqlFiles implements SqlFiles {

    final Map<Path, String> written = new HashMap<>();

    @Override
    public LoadedSql read(Path file) {
        String text = written.getOrDefault(file, "");
        return new LoadedSql(file.getFileName().toString(), text, text.length());
    }

    @Override
    public void write(Path target, String content) {
        written.put(target, content);
    }
}
