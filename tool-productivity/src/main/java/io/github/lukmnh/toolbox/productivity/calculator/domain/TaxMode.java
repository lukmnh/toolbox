package io.github.lukmnh.toolbox.productivity.calculator.domain;

/** Arti nominal yang diketik user. Urutan enum = urutan tampil. */
public enum TaxMode {
    /** Nominal = DPP (harga sebelum pajak); pajak ditambahkan di atasnya. */
    EXCLUSIVE,
    /** Nominal = harga sudah termasuk pajak; DPP dihitung mundur {@code total / (1 + tarif)}. */
    INCLUSIVE
}
