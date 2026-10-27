package io.github.lukmnh.toolbox.json.viewer.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonNodeInfo;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;

/** Tree JSON lazy: dipakai untuk mode tree (root non-array) dan tab Tree di panel detail. */
final class JsonTreeView extends TreeView<TreeEntry> {

    private final JsonTreeItem.ChildLoader loader;

    JsonTreeView(JsonTreeItem.ChildLoader loader) {
        this.loader = loader;
        setCellFactory(view -> new EntryCell());
    }

    void show(JsonNodeInfo root) {
        JsonTreeItem item = new JsonTreeItem(root, loader);
        setRoot(item);
        item.setExpanded(true);
    }

    void clear() {
        setRoot(null);
    }

    /** {@code key: nilai}; object/array menampilkan jumlah anak. */
    static String label(JsonNodeInfo node) {
        String name = node.key() != null
                ? node.key()
                : node.index() >= 0 ? "[" + node.index() + "]" : Messages.get("json.viewer.tree.root");
        String value = switch (node.type()) {
            case OBJECT -> Messages.format("json.viewer.tree.object", node.childCount());
            case ARRAY -> Messages.format("json.viewer.tree.array", node.childCount());
            case STRING -> "\"" + node.preview() + "\"";
            case NUMBER, BOOLEAN, NULL -> node.preview();
        };
        return name + ": " + value;
    }

    private static final class EntryCell extends TreeCell<TreeEntry> {
        @Override
        protected void updateItem(TreeEntry entry, boolean empty) {
            super.updateItem(entry, empty);
            if (empty || entry == null) {
                setText(null);
                return;
            }
            setText(
                    switch (entry) {
                        case TreeEntry.Node node -> label(node.info());
                        case TreeEntry.Message message -> message.text();
                    });
        }
    }
}
