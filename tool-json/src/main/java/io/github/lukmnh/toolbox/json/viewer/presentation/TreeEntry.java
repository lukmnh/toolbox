package io.github.lukmnh.toolbox.json.viewer.presentation;

import io.github.lukmnh.toolbox.json.viewer.domain.JsonNodeInfo;

/** Isi satu baris TreeView: node JSON, atau pesan (memuat…, sisa anak yang tidak ditampilkan). */
sealed interface TreeEntry {

    record Node(JsonNodeInfo info) implements TreeEntry {}

    record Message(String text) implements TreeEntry {}
}
