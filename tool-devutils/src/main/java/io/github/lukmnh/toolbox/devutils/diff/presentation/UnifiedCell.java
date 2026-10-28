package io.github.lukmnh.toolbox.devutils.diff.presentation;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javafx.scene.control.ListCell;

/** Satu baris unified diff dengan warna sesuai jenisnya. */
final class UnifiedCell extends ListCell<UnifiedLine> {

    private static final String TAB_AS_SPACES = "    ";
    private static final String CLASS_PREFIX = "diff-line-";
    private static final List<String> KIND_CLASSES = Arrays.stream(UnifiedLine.Kind.values())
            .map(UnifiedCell::styleClass)
            .toList();

    UnifiedCell() {
        getStyleClass().add("diff-unified-cell");
    }

    @Override
    protected void updateItem(UnifiedLine item, boolean empty) {
        super.updateItem(item, empty);
        getStyleClass().removeAll(KIND_CLASSES);
        setGraphic(null);
        if (empty || item == null) {
            setText(null);
            return;
        }
        setText(item.text().replace("\t", TAB_AS_SPACES));
        getStyleClass().add(styleClass(item.kind()));
    }

    private static String styleClass(UnifiedLine.Kind kind) {
        return CLASS_PREFIX + kind.name().toLowerCase(Locale.ROOT);
    }
}
