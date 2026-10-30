package io.github.lukmnh.toolbox.productivity.calculator.domain;

/** Metode perhitungan bunga cicilan. Urutan enum = urutan tampil. */
public enum InstallmentMethod {
    /** Cicilan tetap; porsi bunga menurun, porsi pokok naik (KPR/KTA umumnya). */
    ANNUITY,
    /** Bunga dari pokok awal sepanjang tenor; cicilan tetap (kredit kendaraan umumnya). */
    FLAT,
    /** Pokok tetap, bunga dari sisa pokok; cicilan menurun. */
    EFFECTIVE
}
