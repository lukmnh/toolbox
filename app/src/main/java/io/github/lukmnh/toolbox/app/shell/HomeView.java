package io.github.lukmnh.toolbox.app.shell;

import io.github.lukmnh.toolbox.app.guide.GuideStore;
import io.github.lukmnh.toolbox.app.guide.GuideView;
import io.github.lukmnh.toolbox.app.settings.ToolUsage;
import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.settings.Theme;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javafx.animation.TranslateTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Home: tab "Tools" (hero + search, favorit bento, recent, grid kartu tool per kategori) dan tab "Guide" (editor +
 * preview panduan).
 */
final class HomeView extends BorderPane {

    private static final double CARD_WIDTH = 260;
    private static final double GAP = 16;
    private static final Duration LIFT_DURATION = Duration.millis(180);
    private static final PseudoClass FAVORITE = PseudoClass.getPseudoClass("favorite");
    private static final PseudoClass FOCUSED = PseudoClass.getPseudoClass("focused");

    private enum Section {
        TOOLS("app.home.tab.tools"),
        GUIDE("app.home.tab.guide");

        private final String messageKey;

        Section(String messageKey) {
            this.messageKey = messageKey;
        }
    }

    private final List<Tool> tools;
    private final ObjectProperty<ToolUsage> usage;
    private final Consumer<Tool> onOpen;
    private final ToolContext context;
    private final ObservableValue<Theme> theme;
    private final ObjectProperty<Section> section = new SimpleObjectProperty<>(Section.TOOLS);
    private final VBox sections = new VBox(GAP * 2);
    private final ScrollPane toolsPane = new ScrollPane();
    private final TextField search = new TextField();
    // Re-render saat favorit berubah (klik bintang); dilepas di dispose() karena Home dibuat ulang tiap kembali
    private final ChangeListener<ToolUsage> usageListener = (observable, previous, current) -> render(search.getText());
    private GuideView guide;

    HomeView(
            List<Tool> tools,
            ObjectProperty<ToolUsage> usage,
            Consumer<Tool> onOpen,
            ToolContext context,
            ObservableValue<Theme> theme) {
        this.tools = tools;
        this.usage = usage;
        this.onOpen = onOpen;
        this.context = context;
        this.theme = theme;
        getStyleClass().add("home-view");

        HBox switcher =
                ToolLayout.segmented(section, List.of(Section.values()), option -> Messages.get(option.messageKey));
        switcher.setPadding(new Insets(GAP * 2, GAP * 2, GAP, GAP * 2));
        setTop(switcher);

        buildToolsPane();
        usage.addListener(usageListener);
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
        search.setPromptText(Messages.get("app.home.search"));
        search.textProperty().addListener((observable, previous, query) -> render(query));

        VBox content = new VBox(GAP * 2, hero(), sections);
        content.setPadding(new Insets(GAP, GAP * 2, GAP * 2, GAP * 2));
        toolsPane.setContent(content);
        render("");
    }

    private VBox hero() {
        Label count = new Label(Messages.format("app.home.count", tools.size()));
        count.getStyleClass().add("home-title");
        Label dot = new Label(".");
        dot.getStyleClass().addAll("home-title", "home-title-dot");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox titleRow = new HBox(new HBox(count, dot), spacer, searchBox());
        titleRow.setAlignment(Pos.BOTTOM_LEFT);
        Label subtitle = new Label(Messages.get("app.home.title"));
        subtitle.getStyleClass().add("home-subtitle");
        VBox hero = new VBox(4, eyebrow("app.home.eyebrow"), titleRow, subtitle);
        hero.getStyleClass().add("home-hero");
        return hero;
    }

    /** TextField tanpa bingkai di dalam box ber-ikon; box ikut state focus karena JavaFX tidak punya :focus-within. */
    private HBox searchBox() {
        Label shortcut = new Label(Messages.get("app.home.shortcut"));
        shortcut.getStyleClass().add("kbd");
        HBox.setHgrow(search, Priority.ALWAYS);
        HBox box = new HBox(8, ToolIcon.glyph("remixmz-search-line"), search, shortcut);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("home-search-box");
        search.focusedProperty()
                .addListener((observable, previous, focused) -> box.pseudoClassStateChanged(FOCUSED, focused));
        return box;
    }

    /** Dipanggil shortcut Ctrl+K: pindah ke tab Tools lalu fokus ke search. */
    void focusSearch() {
        section.set(Section.TOOLS);
        search.requestFocus();
        search.selectAll();
    }

    /** Dipanggil shell saat Home diganti view lain atau aplikasi ditutup. */
    void dispose() {
        usage.removeListener(usageListener);
        if (guide != null) {
            guide.dispose();
        }
    }

    private void render(String query) {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        sections.getChildren().clear();
        // Favorit & recent hanya di tampilan default; saat search, hasil cukup per kategori
        if (needle.isEmpty()) {
            favoritesSection(ToolUsage.resolve(usage.get().favorites(), tools));
            recentSection(ToolUsage.resolve(usage.get().recent(), tools));
        }
        int number = 0;
        for (ToolCategory category : ToolCategory.values()) {
            List<Tool> matches = tools.stream()
                    .filter(tool -> tool.category() == category)
                    .filter(tool -> matches(tool, needle))
                    .toList();
            if (!matches.isEmpty()) {
                sections.getChildren().add(categorySection(++number, category, matches));
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

    /** Bento: favorit pertama selebar dua kartu dengan deskripsi, sisanya kartu ringkas. */
    private void favoritesSection(List<Tool> pinned) {
        if (pinned.isEmpty()) {
            return;
        }
        FlowPane grid = new FlowPane(GAP, GAP);
        for (int i = 0; i < pinned.size(); i++) {
            grid.getChildren().add(card(pinned.get(i), 0, i == 0));
        }
        sections.getChildren().add(new VBox(GAP / 2, eyebrow("app.home.favorites"), grid));
    }

    private void recentSection(List<Tool> recent) {
        if (recent.isEmpty()) {
            return;
        }
        FlowPane pills = new FlowPane(GAP / 2, GAP / 2);
        for (Tool tool : recent) {
            Button pill = new Button(tool.name(), ToolIcon.tile(tool, "tool-icon-small"));
            pill.getStyleClass().add("recent-pill");
            pill.setOnAction(event -> onOpen.accept(tool));
            pills.getChildren().add(pill);
        }
        sections.getChildren().add(new VBox(GAP / 2, eyebrow("app.home.recent"), pills));
    }

    private VBox categorySection(int number, ToolCategory category, List<Tool> categoryTools) {
        Label title = new Label(Messages.get(category.messageKey()));
        title.getStyleClass().add("category-title");
        Label meta =
                new Label(Messages.format("app.home.categoryCount", "%02d".formatted(number), categoryTools.size()));
        meta.getStyleClass().add("category-meta");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(title, spacer, meta);
        header.setAlignment(Pos.BASELINE_LEFT);
        header.getStyleClass().add("section-header");
        FlowPane grid = new FlowPane(GAP, GAP);
        for (int i = 0; i < categoryTools.size(); i++) {
            grid.getChildren().add(card(categoryTools.get(i), i + 1, false));
        }
        return new VBox(GAP, header, grid);
    }

    private static Label eyebrow(String key) {
        Label label = new Label(Messages.get(key).toUpperCase(Locale.ROOT));
        label.getStyleClass().add("section-eyebrow");
        return label;
    }

    /**
     * @param index nomor urut di kategori (01, 02, …); 0 = tanpa nomor (kartu favorit)
     * @param wide kartu bento lebar; kartu favorit lain tampil ringkas tanpa deskripsi
     */
    private VBox card(Tool tool, int index, boolean wide) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(8, ToolIcon.tile(tool, "tool-icon"), spacer);
        top.setAlignment(Pos.CENTER_LEFT);
        if (index > 0) {
            Label number = new Label("%02d".formatted(index));
            number.getStyleClass().add("tool-card-index");
            top.getChildren().add(number);
        }
        top.getChildren().add(favoriteToggle(tool));
        Label name = new Label(tool.name());
        name.getStyleClass().add("tool-card-name");
        VBox card = new VBox(10, top, name);
        if (wide || index > 0) {
            Label description = new Label(tool.description());
            description.getStyleClass().add("tool-card-description");
            description.setWrapText(true);
            card.getChildren().add(description);
        }
        card.getStyleClass().addAll("tool-card", tool.category().styleClass());
        card.setPrefWidth(wide ? CARD_WIDTH * 2 + GAP : CARD_WIDTH);
        card.setOnMouseClicked(event -> onOpen.accept(tool));
        lift(card);
        return card;
    }

    /** Hover = naik 2px dalam 180ms; durasi pendek sesuai pedoman motion desktop. */
    private static void lift(Node node) {
        TranslateTransition transition = new TranslateTransition(LIFT_DURATION, node);
        node.hoverProperty().addListener((observable, previous, hovered) -> {
            transition.stop();
            transition.setToY(hovered ? -2 : 0);
            transition.playFromStart();
        });
    }

    /** Tombol bintang; klik-nya di-consume supaya tidak ikut membuka tool lewat handler kartu. */
    private Button favoriteToggle(Tool tool) {
        boolean favorite = usage.get().isFavorite(tool.id());
        String label = Messages.get(favorite ? "app.home.favorite.remove" : "app.home.favorite.add");
        Button button = new Button();
        button.setGraphic(ToolIcon.glyph(favorite ? "remixmz-star-fill" : "remixmz-star-line"));
        button.getStyleClass().add("favorite-toggle");
        button.pseudoClassStateChanged(FAVORITE, favorite);
        button.setTooltip(new Tooltip(label));
        button.setAccessibleText(label);
        button.setOnAction(event -> usage.set(usage.get().withFavoriteToggled(tool.id())));
        button.setOnMouseClicked(MouseEvent::consume);
        return button;
    }
}
