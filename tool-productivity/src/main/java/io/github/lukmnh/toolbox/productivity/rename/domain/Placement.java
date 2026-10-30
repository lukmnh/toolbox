package io.github.lukmnh.toolbox.productivity.rename.domain;

/** Posisi teks sisipan (nomor, tanggal) terhadap nama. */
public enum Placement {
    START,
    END,
    REPLACE;

    /** Separator hanya dipakai bila nama tidak kosong, supaya tidak ada separator yatim di awal/akhir. */
    String insert(String name, String value, String separator) {
        return switch (this) {
            case START -> name.isEmpty() ? value : value + separator + name;
            case END -> name.isEmpty() ? value : name + separator + value;
            case REPLACE -> value;
        };
    }
}
