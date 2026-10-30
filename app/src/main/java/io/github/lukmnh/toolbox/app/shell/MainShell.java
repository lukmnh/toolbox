package io.github.lukmnh.toolbox.app.shell;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.settings.Theme;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Kerangka window: header (+ toggle tema), sidebar yang bisa disembunyikan, area konten (Home atau tool aktif), dan
 * footer versi.
 */
public final class MainShell extends BorderPane {

    private static final String AUTHOR_URL = "https://github.com/lukmnh";

    private final List<Tool> tools;
    private final ToolContext context;
    private final ObjectProperty<Theme> theme;
    private final ToolNavigator navigator;
    private final Sidebar sidebar;
    private final Label currentTitle = new Label();
    private HomeView home;

    /**
     * @param theme tema aktif; toggle di header mengubah properti ini, pemilik (Application) menerapkan stylesheet dan
     *     menyimpannya
     * @param version versi aplikasi untuk footer
     */
    public MainShell(List<Tool> tools, ToolContext context, ObjectProperty<Theme> theme, String version) {
        this.tools = List.copyOf(tools);
        this.context = context;
        this.theme = theme;
        this.navigator = new ToolNavigator(context, this::setCenter);
        this.sidebar = new Sidebar(this.tools, this::openTool, this::showHome);
        getStyleClass().add("main-shell");
        setTop(header());
        setLeft(sidebar);
        setBottom(footer(version));
        showHomeView();
    }

    private HBox header() {
        Button toggle = new Button("☰");
        toggle.getStyleClass().add("sidebar-toggle");
        toggle.setOnAction(event -> toggleSidebar());
        Label appName = new Label(Messages.get("app.title"));
        appName.getStyleClass().add("app-name");
        currentTitle.getStyleClass().add("current-tool");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(12, toggle, appName, currentTitle, spacer, themeToggle());
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 12, 8, 12));
        header.getStyleClass().add("app-header");
        return header;
    }

    /** Label tombol = tema tujuan, supaya jelas apa yang terjadi saat diklik. */
    private Button themeToggle() {
        Button button = new Button();
        button.getStyleClass().add("theme-toggle");
        button.setTooltip(new Tooltip(Messages.get("app.theme.tooltip")));
        button.textProperty()
                .bind(theme.map(
                        current -> Messages.get(current == Theme.DARK ? "app.theme.toLight" : "app.theme.toDark")));
        button.setOnAction(event -> theme.set(theme.get().toggled()));
        return button;
    }

    private HBox footer(String version) {
        Label versionLabel = new Label(Messages.format("app.footer.version", version));
        Hyperlink author = new Hyperlink(Messages.get("app.footer.author"));
        author.getStyleClass().add("footer-link");
        author.setOnAction(event -> context.hostServices().showDocument(AUTHOR_URL));
        HBox footer = new HBox(8, versionLabel, author);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(4, 12, 4, 12));
        footer.getStyleClass().add("app-footer");
        return footer;
    }

    public void installShortcuts(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN), this::showHome);
        scene.getAccelerators()
                .put(new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN), this::toggleSidebar);
    }

    private void toggleSidebar() {
        setLeft(getLeft() == null ? sidebar : null);
    }

    /** Buka tool; {@code false} bila user membatalkan karena ada perubahan yang belum disimpan. */
    public boolean openTool(Tool tool) {
        if (!navigator.open(tool)) {
            return false;
        }
        disposeHome();
        currentTitle.setText("› " + tool.name());
        sidebar.highlight(Optional.of(tool));
        return true;
    }

    /** Implementasi {@code ToolOpener}: buka tool berdasarkan id lalu serahkan file, kecuali user membatalkan. */
    public void openFile(String toolId, Path file) {
        Tool tool = tools.stream()
                .filter(candidate -> candidate.id().equals(toolId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tool not registered: " + toolId));
        if (openTool(tool)) {
            tool.openFile(file);
        }
    }

    private void showHome() {
        if (navigator.closeActive()) {
            disposeHome();
            showHomeView();
        }
    }

    private void showHomeView() {
        home = new HomeView(tools, this::openTool, context, theme);
        setCenter(home);
        currentTitle.setText("");
        sidebar.highlight(Optional.empty());
    }

    /** Simpan guide yang tertunda dan lepas listener Home sebelum view-nya dibuang. */
    private void disposeHome() {
        if (home != null) {
            home.dispose();
            home = null;
        }
    }

    /** Dipanggil saat window ditutup. */
    public boolean closeActiveTool() {
        return navigator.closeActive();
    }

    /** Dipanggil dari {@code Application.stop()}: lepas tool aktif dan simpan guide yang belum tersimpan. */
    public void disposeActiveTool() {
        navigator.disposeActive();
        disposeHome();
    }
}
