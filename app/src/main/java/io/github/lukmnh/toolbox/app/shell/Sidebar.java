package io.github.lukmnh.toolbox.app.shell;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/** Navigasi samping: Home + daftar tool per kategori; tool aktif di-highlight. */
final class Sidebar extends ScrollPane {

    private static final String ACTIVE = "active";
    private static final double WIDTH = 220;

    private final Map<Tool, Button> buttons = new HashMap<>();
    private final Button homeButton;

    Sidebar(List<Tool> tools, Consumer<Tool> onOpen, Runnable onHome) {
        getStyleClass().add("sidebar");
        setFitToWidth(true);
        setPrefWidth(WIDTH);
        setMinWidth(WIDTH);

        VBox content = new VBox(4);
        content.setPadding(new Insets(12));
        homeButton = navButton(Messages.get("app.nav.home"));
        homeButton.setOnAction(event -> onHome.run());
        content.getChildren().add(homeButton);

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
                Button button = navButton(tool.name());
                button.setOnAction(event -> onOpen.accept(tool));
                buttons.put(tool, button);
                content.getChildren().add(button);
            }
        }
        setContent(content);
    }

    private static Button navButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("sidebar-button");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    void highlight(Optional<Tool> active) {
        homeButton.getStyleClass().remove(ACTIVE);
        buttons.values().forEach(button -> button.getStyleClass().remove(ACTIVE));
        Button target = active.map(buttons::get).orElse(homeButton);
        target.getStyleClass().add(ACTIVE);
    }
}
