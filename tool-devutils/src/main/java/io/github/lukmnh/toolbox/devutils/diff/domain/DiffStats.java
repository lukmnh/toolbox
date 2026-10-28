package io.github.lukmnh.toolbox.devutils.diff.domain;

/** Jumlah baris ditambah, dihapus, dan diubah. */
public record DiffStats(int added, int deleted, int changed) {

    public DiffStats {
        if (added < 0 || deleted < 0 || changed < 0) {
            throw new IllegalArgumentException("Statistik tidak boleh negatif");
        }
    }

    public boolean identical() {
        return added == 0 && deleted == 0 && changed == 0;
    }
}
