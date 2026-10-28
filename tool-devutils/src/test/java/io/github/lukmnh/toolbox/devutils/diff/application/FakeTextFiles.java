package io.github.lukmnh.toolbox.devutils.diff.application;

import io.github.lukmnh.toolbox.devutils.diff.domain.LoadedText;
import io.github.lukmnh.toolbox.devutils.diff.domain.TextFiles;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Fake {@link TextFiles} in-memory. */
final class FakeTextFiles implements TextFiles {

    final Map<Path, String> written = new HashMap<>();

    @Override
    public LoadedText read(Path file) {
        String text = written.getOrDefault(file, "");
        return new LoadedText(file.getFileName().toString(), text, text.length());
    }

    @Override
    public void write(Path target, String content) {
        written.put(target, content);
    }
}
