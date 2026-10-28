package io.github.lukmnh.toolbox.devutils.diff.domain;

import java.util.List;

/** Unified diff standar; {@link #lines()} tanpa newline, {@link #text()} siap disimpan sebagai {@code .diff}. */
public record UnifiedDiff(List<String> lines) {

    public UnifiedDiff {
        lines = List.copyOf(lines);
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public String text() {
        return lines.isEmpty() ? "" : String.join("\n", lines) + "\n";
    }
}
