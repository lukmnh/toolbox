package io.github.lukmnh.toolbox.json.csv.domain;

/** Satu record yang jumlah kolomnya tidak sama dengan lebar tabel. */
public record RowMismatch(long lineNumber, int fieldCount) {}
