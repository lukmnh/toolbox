package io.github.lukmnh.toolbox.app.shell;

import io.github.lukmnh.toolbox.app.settings.AppCache;
import io.github.lukmnh.toolbox.app.settings.ToolUsage;
import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.settings.Theme;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
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
    private final ObjectProperty<ToolUsage> usage;
    private final ToolNavigator navigator;
    private final Sidebar sidebar;
    private final Label currentTitle = new Label();
    private final AppCache cache = AppCache.forCurrentSession();
    private HomeView home;

    /**
     * @param theme tema aktif; toggle di header mengubah properti ini, pemilik (Application) menerapkan stylesheet dan
     *     menyimpannya
     * @param usage favorit dan recent; shell mengubahnya, pemilik (Application) yang menyimpan
     * @param version versi aplikasi untuk footer
     */
    public MainShell(
            List<Tool> tools,
            ToolContext context,
            ObjectProperty<Theme> theme,
            ObjectProperty<ToolUsage> usage,
            String version) {
        this.tools = List.copyOf(tools);
        this.context = context;
        this.theme = theme;
        this.usage = usage;
        this.navigator = new ToolNavigator(context, this::setCenter);
        this.sidebar = new Sidebar(this.tools, usage, this::openTool, this::showHome);
        getStyleClass().add("main-shell");
        setTop(header());
        setLeft(sidebar);
        setBottom(footer(version));
        showHomeView();
    }

    private HBox header() {
        Button toggle = iconButton("remixmz-menu-line", "app.nav.toggle");
        toggle.setOnAction(event -> toggleSidebar());
        Region mark = new Region();
        mark.getStyleClass().add("brand-mark");
        Label appName = new Label(Messages.get("app.title"));
        appName.getStyleClass().add("app-name");
        currentTitle.getStyleClass().add("current-tool");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(12, toggle, mark, appName, currentTitle, spacer, clearCacheButton(), themeToggle());
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 12, 8, 12));
        header.getStyleClass().add("app-header");
        return header;
    }

    /** Tombol ikon tanpa teks: tooltip + accessibleText wajib supaya tetap terbaca screen reader. */
    private static Button iconButton(String literal, String tooltipKey) {
        Button button = new Button();
        button.setGraphic(ToolIcon.glyph(literal));
        button.getStyleClass().add("icon-button");
        button.setTooltip(new Tooltip(Messages.get(tooltipKey)));
        button.setAccessibleText(Messages.get(tooltipKey));
        return button;
    }

    /** Ikon & accessible text = tema tujuan, supaya jelas apa yang terjadi saat diklik. */
    private Button themeToggle() {
        Button button = iconButton("remixmz-moon-line", "app.theme.tooltip");
        button.graphicProperty()
                .bind(theme.map(
                        current -> ToolIcon.glyph(current == Theme.DARK ? "remixmz-sun-line" : "remixmz-moon-line")));
        button.accessibleTextProperty()
                .bind(theme.map(
                        current -> Messages.get(current == Theme.DARK ? "app.theme.toLight" : "app.theme.toDark")));
        button.setOnAction(event -> theme.set(theme.get().toggled()));
        return button;
    }

    private Button clearCacheButton() {
        Button button = iconButton("remixal-delete-bin-line", "app.cache.tooltip");
        button.setOnAction(event -> clearCache());
        return button;
    }

    /** Konfirmasi dulu (dengan ukuran), lalu tutup tool aktif karena temp miliknya ikut terhapus. */
    private void clearCache() {
        boolean confirmed = ErrorDialogs.confirm(
                context.owner(),
                Messages.get("app.cache.confirm.header"),
                Messages.format("app.cache.confirm.detail", ByteSizeFormat.format(cache.size())),
                Messages.get("app.cache.confirm.button"));
        if (!confirmed || !navigator.closeActive()) {
            return;
        }
        disposeHome();
        AppCache.Result result = cache.clear();
        usage.set(usage.get().withoutRecent());
        showHomeView();
        String freed = ByteSizeFormat.format(result.freed());
        if (result.remaining() > 0) {
            context.notifier()
                    .warning(Messages.format("app.cache.partial", freed, ByteSizeFormat.format(result.remaining())));
        } else {
            context.notifier().info(Messages.format("app.cache.done", freed));
        }
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
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN), this::focusSearch);
    }

    /** Ctrl+K dari mana saja: kembali ke Home (bisa dibatalkan user bila ada perubahan) lalu fokus ke search. */
    private void focusSearch() {
        if (home == null) {
            showHome();
        }
        if (home != null) {
            home.focusSearch();
        }
    }

    private void toggleSidebar() {
        setLeft(getLeft() == null ? sidebar : null);
    }

    /** Buka tool; {@code false} bila user membatalkan karena ada perubahan yang belum disimpan. */
    public boolean openTool(Tool tool) {
        if (!navigator.open(tool)) {
            return false;
        }
        usage.set(usage.get().withOpened(tool.id()));
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
        home = new HomeView(tools, usage, this::openTool, context, theme);
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
