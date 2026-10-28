package io.github.lukmnh.toolbox.devutils.diff.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Teks yang dipecah per {@code \n}. Karakter {@code \r} tetap menempel di baris supaya file CRLF bisa
 * direproduksi persis di unified diff.
 *
 * @param endsWithNewline {@code false} bila baris terakhir tidak diakhiri newline (perlu penanda
 *     {@code \ No newline at end of file})
 */
public record TextLines(List<String> lines, boolean endsWithNewline) {

    public TextLines {
        lines = List.copyOf(lines);
    }

    public static TextLines of(String text) {
        if (text.isEmpty()) {
            return new TextLines(List.of(), false);
        }
        List<String> lines = new ArrayList<>();
        int start = 0;
        int newline;
        while ((newline = text.indexOf('\n', start)) >= 0) {
            lines.add(text.substring(start, newline));
            start = newline + 1;
        }
        boolean endsWithNewline = start == text.length();
        if (!endsWithNewline) {
            lines.add(text.substring(start));
        }
        return new TextLines(lines, endsWithNewline);
    }

    public int size() {
        return lines.size();
    }

    public String line(int index) {
        return lines.get(index);
    }

    public boolean isLast(int index) {
        return index == lines.size() - 1;
    }
}
