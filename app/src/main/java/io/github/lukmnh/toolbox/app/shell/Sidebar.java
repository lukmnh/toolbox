package io.github.lukmnh.toolbox.app.shell;

import io.github.lukmnh.toolbox.app.settings.ToolUsage;
import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/** Navigasi samping: Home + favorit + daftar tool per kategori; tool aktif di-highlight. */
final class Sidebar extends ScrollPane {

    private static final String ACTIVE = "active";
    private static final double WIDTH = 220;

    private final Map<Tool, Button> buttons = new HashMap<>();
    private final Map<Tool, Button> favoriteButtons = new HashMap<>();
    private final VBox favorites = new VBox(4);
    private final Button homeButton;
    private final List<Tool> tools;
    private final Consumer<Tool> onOpen;
    private Optional<Tool> active = Optional.empty();

    /** Sidebar hidup sepanjang aplikasi, jadi listener {@code usage} tidak perlu dilepas. */
    Sidebar(List<Tool> tools, ObservableValue<ToolUsage> usage, Consumer<Tool> onOpen, Runnable onHome) {
        this.tools = tools;
        this.onOpen = onOpen;
        getStyleClass().add("sidebar");
        setFitToWidth(true);
        setPrefWidth(WIDTH);
        setMinWidth(WIDTH);

        VBox content = new VBox(4);
        content.setPadding(new Insets(12));
        homeButton = navButton(Messages.get("app.nav.home"));
        homeButton.setGraphic(ToolIcon.glyph("remixal-home-5-line"));
        homeButton.setOnAction(event -> onHome.run());
        content.getChildren().addAll(homeButton, favorites);

        for (ToolCategory category : ToolCategory.values()) {
            List<Tool> categoryTools =
                    tools.stream().filter(tool -> tool.category() == category).toList();
            if (categoryTools.isEmpty()) {
                continue;
            }
            Label heading = new Label(Messages.get(category.messageKey()));
            heading.getStyleClass().add("sidebar-category");
            content.getChildren().add(heading);
            for (Tool tool : categoryTools) {
                Button button = navButton(tool);
                buttons.put(tool, button);
                content.getChildren().add(button);
            }
        }
        setContent(content);
        renderFavorites(usage.getValue());
        usage.addListener((observable, previous, current) -> renderFavorites(current));
    }

    private static Button navButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("sidebar-button");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    /** Tombol tool dengan ikon kecil berwarna kategori. */
    private Button navButton(Tool tool) {
        Button button = navButton(tool.name());
        button.setGraphic(ToolIcon.tile(tool, "tool-icon-small"));
        button.setOnAction(event -> onOpen.accept(tool));
        return button;
    }

    private void renderFavorites(ToolUsage usage) {
        favoriteButtons.clear();
        favorites.getChildren().clear();
        List<Tool> pinned = ToolUsage.resolve(usage.favorites(), tools);
        if (pinned.isEmpty()) {
            return;
        }
        Label heading = new Label(Messages.get("app.nav.favorites"));
        heading.getStyleClass().add("sidebar-category");
        favorites.getChildren().add(heading);
        for (Tool tool : pinned) {
            Button button = navButton(tool);
            favoriteButtons.put(tool, button);
            favorites.getChildren().add(button);
        }
        highlight(active);
    }

    /** Tool favorit bisa muncul dua kali (Favorites + kategorinya); keduanya di-highlight. */
    void highlight(Optional<Tool> tool) {
        active = tool;
        homeButton.getStyleClass().remove(ACTIVE);
        Stream.concat(buttons.values().stream(), favoriteButtons.values().stream())
                .forEach(button -> button.getStyleClass().remove(ACTIVE));
        if (tool.isEmpty()) {
            homeButton.getStyleClass().add(ACTIVE);
            return;
        }
        Stream.of(buttons.get(tool.get()), favoriteButtons.get(tool.get()))
                .filter(Objects::nonNull)
                .forEach(button -> button.getStyleClass().add(ACTIVE));
    }
}
