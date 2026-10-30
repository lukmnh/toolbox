package io.github.lukmnh.toolbox.app.shell;

import io.github.lukmnh.toolbox.app.guide.GuideStore;
import io.github.lukmnh.toolbox.app.guide.GuideView;
import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.settings.Theme;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Home: tab "Tools" (search + grid kartu tool per kategori) dan tab "Guide" (editor + preview panduan). */
final class HomeView extends BorderPane {

    private static final double CARD_WIDTH = 260;
    private static final double GAP = 16;

    private enum Section {
        TOOLS("app.home.tab.tools"),
        GUIDE("app.home.tab.guide");

        private final String messageKey;

        Section(String messageKey) {
            this.messageKey = messageKey;
        }
    }

    private final List<Tool> tools;
    private final Consumer<Tool> onOpen;
    private final ToolContext context;
    private final ObservableValue<Theme> theme;
    private final VBox sections = new VBox(GAP * 1.5);
    private final ScrollPane toolsPane = new ScrollPane();
    private GuideView guide;

    HomeView(List<Tool> tools, Consumer<Tool> onOpen, ToolContext context, ObservableValue<Theme> theme) {
        this.tools = tools;
        this.onOpen = onOpen;
        this.context = context;
        this.theme = theme;
        getStyleClass().add("home-view");

        ObjectProperty<Section> section = new SimpleObjectProperty<>(Section.TOOLS);
        HBox switcher =
                ToolLayout.segmented(section, List.of(Section.values()), option -> Messages.get(option.messageKey));
        switcher.setPadding(new Insets(GAP * 2, GAP * 2, GAP, GAP * 2));
        setTop(switcher);

        buildToolsPane();
        section.addListener((observable, previous, current) -> show(current));
        show(section.get());
    }

    private void show(Section section) {
        if (section == Section.TOOLS) {
            setCenter(toolsPane);
            return;
        }
        // Lazy: WebView + baca file guide hanya saat tab Guide dibuka
        if (guide == null) {
            guide = new GuideView(context, theme, GuideStore.inDataDirectory());
        }
        setCenter(guide);
    }

    private void buildToolsPane() {
        toolsPane.getStyleClass().add("home-view");
        toolsPane.setFitToWidth(true);

        Label title = new Label(Messages.get("app.home.title"));
        title.getStyleClass().add("home-title");
        TextField search = new TextField();
        search.setPromptText(Messages.get("app.home.search"));
        search.getStyleClass().add("home-search");
        search.textProperty().addListener((observable, previous, query) -> render(query));

        VBox content = new VBox(GAP, title, search, sections);
        content.setPadding(new Insets(GAP, GAP * 2, GAP * 2, GAP * 2));
        toolsPane.setContent(content);
        render("");
    }

    /** Dipanggil shell saat Home diganti view lain atau aplikasi ditutup. */
    void dispose() {
        if (guide != null) {
            guide.dispose();
        }
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
