package io.github.lukmnh.toolbox.app.shell;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/** Home: kotak search + grid kartu tool per kategori. */
final class HomeView extends ScrollPane {

    private static final double CARD_WIDTH = 260;
    private static final double GAP = 16;

    private final List<Tool> tools;
    private final Consumer<Tool> onOpen;
    private final VBox sections = new VBox(GAP * 1.5);

    HomeView(List<Tool> tools, Consumer<Tool> onOpen) {
        this.tools = tools;
        this.onOpen = onOpen;
        getStyleClass().add("home-view");
        setFitToWidth(true);

        Label title = new Label(Messages.get("app.home.title"));
        title.getStyleClass().add("home-title");
        TextField search = new TextField();
        search.setPromptText(Messages.get("app.home.search"));
        search.getStyleClass().add("home-search");
        search.textProperty().addListener((observable, previous, query) -> render(query));

        VBox content = new VBox(GAP, title, search, sections);
        content.setPadding(new Insets(GAP * 2));
        setContent(content);
        render("");
    }

    private void render(String query) {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        sections.getChildren().clear();
        for (ToolCategory category : ToolCategory.values()) {
            List<Tool> matches = tools.stream()
                    .filter(tool -> tool.category() == category)
                    .filter(tool -> matches(tool, needle))
                    .toList();
            if (!matches.isEmpty()) {
                sections.getChildren().add(section(category, matches));
            }
        }
        if (sections.getChildren().isEmpty()) {
            sections.getChildren().add(new Label(Messages.get("app.home.noResult")));
        }
    }

    private static boolean matches(Tool tool, String needle) {
        return needle.isEmpty()
                || tool.name().toLowerCase(Locale.ROOT).contains(needle)
                || tool.description().toLowerCase(Locale.ROOT).contains(needle);
    }

    private VBox section(ToolCategory category, List<Tool> categoryTools) {
        Label heading = new Label(Messages.get(category.messageKey()));
        heading.getStyleClass().add("category-title");
        FlowPane grid = new FlowPane(GAP, GAP);
        categoryTools.forEach(tool -> grid.getChildren().add(card(tool)));
        return new VBox(GAP / 2, heading, grid);
    }

    private VBox card(Tool tool) {
        Label name = new Label(tool.name());
        name.getStyleClass().add("tool-card-name");
        Label description = new Label(tool.description());
        description.getStyleClass().add("tool-card-description");
        description.setWrapText(true);
        VBox card = new VBox(6, name, description);
        card.getStyleClass().add("tool-card");
        card.setPrefWidth(CARD_WIDTH);
        card.setOnMouseClicked(event -> onOpen.accept(tool));
        return card;
    }
}
