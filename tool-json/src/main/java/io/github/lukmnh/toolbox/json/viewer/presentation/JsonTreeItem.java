package io.github.lukmnh.toolbox.json.viewer.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonNodeInfo;
import io.github.lukmnh.toolbox.json.viewer.domain.NodeChildren;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.scene.control.TreeItem;

/** Node tree yang anaknya baru dibaca dari file saat pertama kali di-expand. */
final class JsonTreeItem extends TreeItem<TreeEntry> {

    /** Memuat anak node di background lalu memanggil callback di FX thread. */
    @FunctionalInterface
    interface ChildLoader {
        void load(JsonNodeInfo node, Consumer<NodeChildren> onLoaded);
    }

    private final JsonNodeInfo node;
    private final ChildLoader loader;
    private boolean requested;

    JsonTreeItem(JsonNodeInfo node, ChildLoader loader) {
        super(new TreeEntry.Node(node));
        this.node = node;
        this.loader = loader;
        if (node.hasChildren()) {
            // Placeholder supaya panah expand muncul sebelum anak dibaca
            getChildren().add(message(Messages.get("json.viewer.tree.loading")));
            expandedProperty().addListener((observable, wasExpanded, expanded) -> {
                if (expanded) {
                    loadOnce();
                }
            });
        }
    }

    private void loadOnce() {
        if (requested) {
            return;
        }
        requested = true;
        loader.load(node, this::showChildren);
    }

    private void showChildren(NodeChildren children) {
        List<TreeItem<TreeEntry>> items = new ArrayList<>(children.nodes().size() + 1);
        for (JsonNodeInfo child : children.nodes()) {
            items.add(new JsonTreeItem(child, loader));
        }
        if (children.hiddenCount() > 0) {
            items.add(message(Messages.format("json.viewer.tree.hidden", children.hiddenCount())));
        }
        getChildren().setAll(items);
    }

    private static TreeItem<TreeEntry> message(String text) {
        return new TreeItem<>(new TreeEntry.Message(text));
    }
}
