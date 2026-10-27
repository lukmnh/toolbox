package io.github.lukmnh.toolbox.common.download;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.AtomicFiles;
import io.github.lukmnh.toolbox.common.settings.Settings;
import io.github.lukmnh.toolbox.common.task.TaskRunner;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.common.ui.NotificationAction;
import io.github.lukmnh.toolbox.common.ui.Notifier;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javafx.application.HostServices;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * Menyalin hasil tool (yang sudah ada di temp file) ke lokasi user.
 *
 * <p>File sumber tidak dipindah, sehingga user bisa men-download ulang. Penyalinan berjalan di background,
 * lalu toast "Buka file / Buka folder" ditampilkan. Semua method harus dipanggil dari FX thread.
 */
public final class DownloadService {

    private final Supplier<Settings> settings;
    private final Supplier<Window> owner;
    private final Notifier notifier;
    private final TaskRunner tasks;
    private final HostServices hostServices;

    public DownloadService(
            Supplier<Settings> settings,
            Supplier<Window> owner,
            Notifier notifier,
            TaskRunner tasks,
            HostServices hostServices) {
        this.settings = settings;
        this.owner = owner;
        this.notifier = notifier;
        this.tasks = tasks;
        this.hostServices = hostServices;
    }

    /** Tombol utama: simpan ke folder download default dengan nama unik. */
    public void download(Path source, String suggestedName) {
        if (settings.get().alwaysAskLocation()) {
            saveAs(source, suggestedName);
            return;
        }
        Path directory = settings.get().downloadDirectory();
        deliverFile(source, suggestedName, () -> UniqueFileNamer.uniqueFile(directory, suggestedName));
    }

    /** "Simpan sebagai…": user memilih lokasi dan nama. */
    public void saveAs(Path source, String suggestedName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Messages.get("download.saveAs.title"));
        chooser.setInitialFileName(suggestedName);
        existingDownloadDirectory().ifPresent(chooser::setInitialDirectory);
        extensionFilter(suggestedName).ifPresent(chooser.getExtensionFilters()::add);
        File chosen = chooser.showSaveDialog(owner.get());
        if (chosen == null) {
            return;
        }
        Path target = withExtensionOf(chosen.toPath(), suggestedName);
        deliverFile(source, suggestedName, () -> target);
    }

    /** Output multi-file: isi {@code sourceDirectory} disalin ke subfolder unik di folder download. */
    public void downloadDirectory(Path sourceDirectory, String suggestedDirectoryName) {
        if (settings.get().alwaysAskLocation()) {
            saveDirectoryAs(sourceDirectory, suggestedDirectoryName);
            return;
        }
        deliverDirectory(sourceDirectory, suggestedDirectoryName, settings.get().downloadDirectory());
    }

    public void saveDirectoryAs(Path sourceDirectory, String suggestedDirectoryName) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(Messages.get("download.saveFolderAs.title"));
        existingDownloadDirectory().ifPresent(chooser::setInitialDirectory);
        File chosen = chooser.showDialog(owner.get());
        if (chosen != null) {
            deliverDirectory(sourceDirectory, suggestedDirectoryName, chosen.toPath());
        }
    }

    private void deliverFile(Path source, String displayName, Supplier<Path> targetResolver) {
        tasks.submit(
                (progress, cancellation) -> {
                    Path target = targetResolver.get();
                    Files.createDirectories(target.toAbsolutePath().getParent());
                    AtomicFiles.copy(source, target);
                    return target;
                },
                this::notifyFileSaved,
                error -> showError(displayName, error));
    }

    private void deliverDirectory(Path sourceDirectory, String directoryName, Path parent) {
        tasks.submit(
                (progress, cancellation) -> {
                    Path target = UniqueFileNamer.uniqueDirectory(parent, directoryName);
                    Files.createDirectories(target);
                    copyFiles(sourceDirectory, target);
                    return target;
                },
                this::notifyDirectorySaved,
                error -> showError(directoryName, error));
    }

    private static void copyFiles(Path sourceDirectory, Path targetDirectory) throws IOException {
        List<Path> files;
        try (Stream<Path> entries = Files.list(sourceDirectory)) {
            files = entries.filter(Files::isRegularFile).sorted().toList();
        }
        for (Path file : files) {
            AtomicFiles.copy(file, targetDirectory.resolve(file.getFileName().toString()));
        }
    }

    private void notifyFileSaved(Path target) {
        notifier.success(
                Messages.format("download.success", target.getFileName()),
                List.of(
                        new NotificationAction(Messages.get("download.openFile"), () -> open(target)),
                        new NotificationAction(Messages.get("download.openFolder"), () -> open(target.getParent()))));
    }

    private void notifyDirectorySaved(Path target) {
        notifier.success(
                Messages.format("download.success", target.getFileName()),
                List.of(new NotificationAction(Messages.get("download.openFolder"), () -> open(target))));
    }

    private void open(Path path) {
        hostServices.showDocument(path.toUri().toString());
    }

    private void showError(String name, Throwable error) {
        ErrorDialogs.show(
                owner.get(),
                Messages.get("download.error.title"),
                Messages.format("download.error.message", name, ErrorDialogs.describe(error)));
    }

    private Optional<File> existingDownloadDirectory() {
        Path directory = settings.get().downloadDirectory();
        return Files.isDirectory(directory) ? Optional.of(directory.toFile()) : Optional.empty();
    }

    private static Optional<FileChooser.ExtensionFilter> extensionFilter(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
            return Optional.empty();
        }
        String extension = fileName.substring(dot);
        return Optional.of(new FileChooser.ExtensionFilter("*" + extension, "*" + extension));
    }

    /** Dialog GTK tidak selalu menambahkan ekstensi; tambahkan bila user tidak menulisnya. */
    private static Path withExtensionOf(Path chosen, String suggestedName) {
        int dot = suggestedName.lastIndexOf('.');
        String chosenName = chosen.getFileName().toString();
        if (dot <= 0 || chosenName.contains(".")) {
            return chosen;
        }
        return chosen.resolveSibling(chosenName + suggestedName.substring(dot));
    }
}
