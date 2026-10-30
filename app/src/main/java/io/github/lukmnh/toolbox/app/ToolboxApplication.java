package io.github.lukmnh.toolbox.app;

import io.github.lukmnh.toolbox.app.routing.OpenWithRouter;
import io.github.lukmnh.toolbox.app.settings.ThemeStore;
import io.github.lukmnh.toolbox.app.shell.MainShell;
import io.github.lukmnh.toolbox.app.shell.ToolRegistry;
import io.github.lukmnh.toolbox.common.download.DownloadLocationResolver;
import io.github.lukmnh.toolbox.common.download.DownloadService;
import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.AppDirectories;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.security.OwnerOnlyFiles;
import io.github.lukmnh.toolbox.common.settings.Settings;
import io.github.lukmnh.toolbox.common.settings.Theme;
import io.github.lukmnh.toolbox.common.task.TaskRunner;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.common.ui.ToastNotifier;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Bootstrap: merakit layanan shell, memuat tool lewat ServiceLoader, lalu menampilkan window. */
public final class ToolboxApplication extends Application {

    private static final System.Logger LOG = System.getLogger(ToolboxApplication.class.getName());
    private static final double MIN_WIDTH = 1200;
    private static final double MIN_HEIGHT = 800;
    private static final String LIGHT_THEME = "/css/theme-light.css";
    private static final String DARK_THEME = "/css/theme-dark.css";

    private TaskRunner taskRunner;
    private MainShell shell;

    @Override
    public void init() {
        prepareDataDirectory();
    }

    /** {@code ~/.toolbox} owner-only, lalu bersihkan sisa temp dari sesi yang crash (Security Requirements #4). */
    private static void prepareDataDirectory() {
        try {
            OwnerOnlyFiles.createDirectories(AppDirectories.dataDirectory());
        } catch (IOException e) {
            LOG.log(Level.ERROR, "Gagal membatasi permission folder data aplikasi", e);
        }
        Path temp = AppDirectories.tempDirectory();
        TempFiles.purge(temp);
        // Tetap jalan walau app ditutup tanpa stop() (misalnya SIGTERM)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> TempFiles.purge(temp), "toolbox-temp-cleanup"));
    }

    @Override
    public void start(Stage stage) {
        Settings defaults =
                Settings.defaults(DownloadLocationResolver.forCurrentSystem().resolve());
        ThemeStore themeStore = ThemeStore.inDataDirectory();
        ObjectProperty<Theme> theme = new SimpleObjectProperty<>(themeStore.load());
        // Supplier, bukan snapshot: tool selalu melihat tema terkini setelah user menekan toggle
        Supplier<Settings> settings = () -> defaults.withTheme(theme.get());
        StackPane root = new StackPane();
        taskRunner = new TaskRunner();
        installUncaughtExceptionHandler(stage);

        List<Tool> tools = ToolRegistry.load().tools();
        // Notifier dibuat setelah shell supaya toast selalu tampil di atas konten
        StackPane shellHost = new StackPane();
        root.getChildren().add(shellHost);
        ToastNotifier notifier = new ToastNotifier(root);
        DownloadService downloads = new DownloadService(settings, () -> stage, notifier, taskRunner, getHostServices());
        ToolContext context = new ToolContext(
                downloads,
                taskRunner,
                notifier,
                settings,
                getHostServices(),
                stage,
                // shell dibuat setelah context; lambda baru dipanggil saat user menekan tombol di tool
                (toolId, file) -> shell.openFile(toolId, file));
        shell = new MainShell(tools, context, theme, AppVersion.current());
        shellHost.getChildren().add(shell);

        Scene scene = new Scene(root, MIN_WIDTH, MIN_HEIGHT);
        scene.getStylesheets().add(stylesheet(LIGHT_THEME));
        applyTheme(scene, theme.get());
        theme.addListener((observable, previous, current) -> {
            applyTheme(scene, current);
            saveTheme(themeStore, current, notifier);
        });
        shell.installShortcuts(scene);

        stage.setTitle(Messages.get("app.title"));
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            if (!shell.closeActiveTool()) {
                event.consume();
            }
        });
        stage.show();

        new OpenWithRouter(tools).route(getParameters().getRaw(), shell::openTool, notifier::warning);
    }

    /** Tema gelap = theme-light.css + theme-dark.css (hanya menimpa variabel warna). */
    private void applyTheme(Scene scene, Theme theme) {
        String dark = stylesheet(DARK_THEME);
        if (theme == Theme.DARK && !scene.getStylesheets().contains(dark)) {
            scene.getStylesheets().add(dark);
        } else if (theme != Theme.DARK) {
            scene.getStylesheets().remove(dark);
        }
    }

    /** Gagal simpan tidak membatalkan pergantian tema; user diberi tahu bahwa pilihan tidak akan bertahan. */
    private static void saveTheme(ThemeStore store, Theme theme, ToastNotifier notifier) {
        try {
            store.save(theme);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Gagal menyimpan pilihan tema", e);
            notifier.warning(Messages.format("app.theme.saveError", ErrorDialogs.describe(e)));
        }
    }

    private String stylesheet(String path) {
        return Objects.requireNonNull(getClass().getResource(path), path).toExternalForm();
    }

    private static void installUncaughtExceptionHandler(Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            LOG.log(Level.ERROR, "Uncaught exception di thread " + thread.getName(), error);
            if (Platform.isFxApplicationThread()) {
                ErrorDialogs.showUnexpected(stage, error);
            } else {
                Platform.runLater(() -> ErrorDialogs.showUnexpected(stage, error));
            }
        });
    }

    @Override
    public void stop() {
        if (shell != null) {
            shell.disposeActiveTool();
        }
        if (taskRunner != null) {
            taskRunner.close();
        }
    }
}
