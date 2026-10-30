package io.github.lukmnh.toolbox.app.shell;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.BackgroundService;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Kerangka window: header (+ indikator background service), sidebar yang bisa disembunyikan, dan area konten (Home
 * atau tool aktif).
 */
public final class MainShell extends BorderPane {

    private final List<Tool> tools;
    private final List<BackgroundService> backgroundServices;
    private final ToolContext context;
    private final ToolNavigator navigator;
    private final Sidebar sidebar;
    private final Label currentTitle = new Label();

    public MainShell(List<Tool> tools, List<BackgroundService> backgroundServices, ToolContext context) {
        this.tools = List.copyOf(tools);
        this.backgroundServices = List.copyOf(backgroundServices);
        this.context = context;
        this.navigator = new ToolNavigator(context, this::setCenter);
        this.sidebar = new Sidebar(this.tools, this::openTool, this::showHome);
        getStyleClass().add("main-shell");
        setTop(header());
        setLeft(sidebar);
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
        HBox header = new HBox(12, toggle, appName, currentTitle, spacer);
        backgroundServices.forEach(service -> header.getChildren().add(indicator(service)));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 12, 8, 12));
        header.getStyleClass().add("app-header");
        return header;
    }

    /** Indikator hanya tampil (dan memakan tempat) selama service berjalan. */
    private Node indicator(BackgroundService service) {
        Node indicator = service.statusIndicator(context);
        indicator.visibleProperty().bind(service.runningProperty());
        indicator.managedProperty().bind(indicator.visibleProperty());
        return indicator;
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
        currentTitle.setText("› " + tool.name());
        sidebar.highlight(Optional.of(tool));
        return true;
    }

    /** Implementasi {@code ToolOpener}: buka tool berdasarkan id lalu serahkan file, kecuali user membatalkan. */
    public void openFile(String toolId, Path file) {
        Tool tool = tools.stream()
                .filter(candidate -> candidate.id().equals(toolId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tool tidak terdaftar: " + toolId));
        if (openTool(tool)) {
            tool.openFile(file);
        }
    }

    private void showHome() {
        if (navigator.closeActive()) {
            showHomeView();
        }
    }

    private void showHomeView() {
        Node home = new HomeView(tools, this::openTool);
        setCenter(home);
        currentTitle.setText("");
        sidebar.highlight(Optional.empty());
    }

    /** Dipanggil saat window ditutup. */
    public boolean closeActiveTool() {
        return navigator.closeActive();
    }

    public void disposeActiveTool() {
        navigator.disposeActive();
    }
}
