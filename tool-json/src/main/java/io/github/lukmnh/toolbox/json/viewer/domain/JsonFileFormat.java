package io.github.lukmnh.toolbox.json.viewer.domain;

/** Bentuk file JSON yang menentukan mode tampilan Viewer. */
public enum JsonFileFormat {
    /** Array top-level: tiap elemen = satu record, mode tabel. */
    ARRAY,
    /** JSONL/NDJSON (atau beberapa nilai root): tiap baris = satu record, mode tabel. */
    JSON_LINES,
    /** Satu nilai root non-array: mode tree. */
    SINGLE_VALUE;

    public boolean isTable() {
        return this != SINGLE_VALUE;
    }
}
