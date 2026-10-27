package io.github.lukmnh.toolbox.common.tool;

import io.github.lukmnh.toolbox.common.download.DownloadService;
import io.github.lukmnh.toolbox.common.settings.Settings;
import io.github.lukmnh.toolbox.common.task.TaskRunner;
import io.github.lukmnh.toolbox.common.ui.Notifier;
import java.util.Objects;
import java.util.function.Supplier;
import javafx.application.HostServices;
import javafx.stage.Window;

/** Layanan shell yang tersedia untuk setiap tool. */
public record ToolContext(
        DownloadService downloads,
        TaskRunner tasks,
        Notifier notifier,
        Supplier<Settings> settings,
        HostServices hostServices,
        Window owner) {

    public ToolContext {
        Objects.requireNonNull(downloads, "downloads");
        Objects.requireNonNull(tasks, "tasks");
        Objects.requireNonNull(notifier, "notifier");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(hostServices, "hostServices");
        Objects.requireNonNull(owner, "owner");
    }
}
