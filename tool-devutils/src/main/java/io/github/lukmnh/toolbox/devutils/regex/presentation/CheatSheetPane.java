package io.github.lukmnh.toolbox.devutils.regex.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexFlavor;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevLayout;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/** Tab Cheat sheet: section yang bisa di-collapse, isinya hanya sintaks yang berlaku di flavor aktif. */
final class CheatSheetPane {

    private static final double TOKEN_COLUMN_WIDTH = 170;

    private final RegexViewModel viewModel;
    private final VBox sections = new VBox(DevLayout.GAP);

    private CheatSheetPane(RegexViewModel viewModel) {
        this.viewModel = viewModel;
    }

    static Node create(RegexViewModel viewModel) {
        return new CheatSheetPane(viewModel).build();
    }

    private Node build() {
        sections.setPadding(new Insets(DevLayout.GAP));
        viewModel.flavorProperty().addListener((observable, previous, current) -> rebuild(current));
        rebuild(viewModel.flavorProperty().get());
        ScrollPane scroll = new ScrollPane(sections);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("edge-to-edge");
        return scroll;
    }

    private void rebuild(RegexFlavor flavor) {
        sections.getChildren().clear();
        for (CheatSheet.Category category : CheatSheet.Category.values()) {
            List<CheatSheet.Entry> entries = CheatSheet.entries(flavor, category);
            if (entries.isEmpty()) {
                continue;
            }
            TitledPane section =
                    new TitledPane(Messages.get("devutils.regex.cheat.category." + category.name()), grid(entries));
            // Section pertama terbuka sebagai contoh; sisanya ditutup supaya tab tidak panjang
            section.setExpanded(sections.getChildren().isEmpty());
            section.getStyleClass().add("regex-cheat-section");
            sections.getChildren().add(section);
        }
    }

    private static GridPane grid(List<CheatSheet.Entry> entries) {
        GridPane grid = new GridPane();
        grid.setHgap(DevLayout.SPACING);
        grid.setVgap(DevLayout.GAP);
        for (int row = 0; row < entries.size(); row++) {
            CheatSheet.Entry entry = entries.get(row);
            Label token = new Label(entry.token());
            token.getStyleClass().add("regex-token");
            token.setMinWidth(TOKEN_COLUMN_WIDTH);
            Label description = new Label(Messages.get("devutils.regex.cheat." + entry.id()));
            description.setWrapText(true);
            grid.addRow(row, token, description);
        }
        return grid;
    }
}
