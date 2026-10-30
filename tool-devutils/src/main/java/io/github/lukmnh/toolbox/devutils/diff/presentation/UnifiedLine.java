package io.github.lukmnh.toolbox.devutils.diff.presentation;

import java.util.ArrayList;
import java.util.List;

/**
 * Satu baris unified diff beserta jenisnya. Jenis ditentukan dari posisi, bukan hanya prefix, karena baris yang
 * dihapus bisa berbunyi {@code "--- komentar"} dan tidak boleh dikira header.
 */
record UnifiedLine(String text, Kind kind) {

    enum Kind {
        HEADER,
        HUNK,
        INSERT,
        DELETE,
        CONTEXT,
        NOTE
    }

    private static final int HEADER_LINES = 2;

    static List<UnifiedLine> classify(List<String> lines) {
        List<UnifiedLine> classified = new ArrayList<>(lines.size());
        for (int index = 0; index < lines.size(); index++) {
            String line = stripCarriageReturn(lines.get(index));
            classified.add(new UnifiedLine(line, index < HEADER_LINES ? Kind.HEADER : kindOf(line)));
        }
        return classified;
    }

    private static Kind kindOf(String line) {
        if (line.startsWith("@@")) {
            return Kind.HUNK;
        }
        if (line.isEmpty()) {
            return Kind.CONTEXT;
        }
        return switch (line.charAt(0)) {
            case '+' -> Kind.INSERT;
            case '-' -> Kind.DELETE;
            case '\\' -> Kind.NOTE;
            default -> Kind.CONTEXT;
        };
    }

    private static String stripCarriageReturn(String line) {
        return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
    }
}
