package io.github.lukmnh.toolbox.productivity.budget.domain;

/** Item nonaktif (misalnya barang yang sudah tidak dipakai) tetap tercatat tapi tidak masuk total. */
public enum ItemStatus {
    ACTIVE,
    INACTIVE;

    public boolean counts() {
        return this == ACTIVE;
    }
}
