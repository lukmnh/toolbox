package io.github.lukmnh.toolbox.json.viewer.domain;

import java.util.Objects;

/**
 * Satu node di mode tree. Anak node tidak disimpan; dibaca ulang dari rentang byte {@code [start, end)} saat
 * node di-expand.
 *
 * @param key nama key bila node adalah field object, {@code null} untuk elemen array atau root
 * @param index posisi di array induk, -1 bila bukan elemen array
 * @param preview nilai scalar (maks 200 karakter); kosong untuk object/array
 * @param childCount jumlah anak langsung untuk object/array
 */
public record JsonNodeInfo(String key, int index, NodeType type, String preview, int childCount, long start, long end) {

    public static final int PREVIEW_MAX_CHARS = 200;

    public JsonNodeInfo {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(preview, "preview");
    }

    public boolean hasChildren() {
        return type.isContainer() && childCount > 0;
    }
}
