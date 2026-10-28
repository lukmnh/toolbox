package io.github.lukmnh.toolbox.devutils.diff.domain;

import java.util.ArrayList;
import java.util.List;

/** Statistik dan posisi awal blok perubahan, dihitung dari baris side-by-side. */
public final class DiffSummary {

    private DiffSummary() {}

    public static DiffStats stats(List<DiffRow> rows) {
        int added = 0;
        int deleted = 0;
        int changed = 0;
        for (DiffRow row : rows) {
            switch (row.type()) {
                case INSERT -> added++;
                case DELETE -> deleted++;
                case CHANGE -> changed++;
                case EQUAL -> {
                    // Baris sama tidak dihitung
                }
            }
        }
        return new DiffStats(added, deleted, changed);
    }

    public static List<Integer> changeStarts(List<DiffRow> rows) {
        List<Integer> starts = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            boolean changed = rows.get(index).type() != RowType.EQUAL;
            boolean previousEqual = index == 0 || rows.get(index - 1).type() == RowType.EQUAL;
            if (changed && previousEqual) {
                starts.add(index);
            }
        }
        return starts;
    }
}
