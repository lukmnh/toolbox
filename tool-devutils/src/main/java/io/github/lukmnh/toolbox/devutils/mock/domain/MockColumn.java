package io.github.lukmnh.toolbox.devutils.mock.domain;

/** Satu kolom output: nama, tipe, dan opsi tipe (range, daftar enum, atau pattern). */
public record MockColumn(String name, MockType type, String options) {

    /** Opsi kosong = default tipe, supaya user tidak wajib mengisi. */
    public String effectiveOptions() {
        return options == null || options.isBlank() ? type.defaultOptions() : options.strip();
    }
}
