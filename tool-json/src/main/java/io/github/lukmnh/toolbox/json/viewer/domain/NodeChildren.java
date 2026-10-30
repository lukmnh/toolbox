package io.github.lukmnh.toolbox.json.viewer.domain;

import java.util.List;

/** @param totalCount jumlah anak sebenarnya; bisa lebih banyak dari {@code nodes} bila dibatasi */
public record NodeChildren(List<JsonNodeInfo> nodes, int totalCount) {

    public NodeChildren {
        nodes = List.copyOf(nodes);
    }

    public int hiddenCount() {
        return totalCount - nodes.size();
    }
}
