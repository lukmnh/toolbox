package io.github.lukmnh.toolbox.productivity.budget.infrastructure;

import java.util.HashMap;
import java.util.Map;
import org.dhatim.fastexcel.Worksheet;

/** Lebar kolom disesuaikan isi terpanjang (FastExcel tidak auto-fit), dengan batas bawah/atas supaya tetap rapi. */
final class ColumnWidths {

    private static final double MIN_WIDTH = 8;
    private static final double MAX_WIDTH = 60;
    private static final double PADDING = 2;

    private final Map<Integer, Integer> longest = new HashMap<>();

    void track(int column, String text) {
        int length = text.codePointCount(0, text.length());
        longest.merge(column, length, Math::max);
    }

    /** Angka hasil format Rupiah diperkirakan dari panjang teks contohnya. */
    void trackNumber(int column, int displayLength) {
        longest.merge(column, displayLength, Math::max);
    }

    void apply(Worksheet sheet) {
        longest.forEach((column, length) -> sheet.width(column, Math.clamp(length + PADDING, MIN_WIDTH, MAX_WIDTH)));
    }
}
