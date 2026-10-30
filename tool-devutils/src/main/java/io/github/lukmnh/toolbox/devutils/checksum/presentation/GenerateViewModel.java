package io.github.lukmnh.toolbox.devutils.checksum.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumAlgorithm;
import io.github.lukmnh.toolbox.devutils.checksum.domain.FileChecksum;
import io.github.lukmnh.toolbox.devutils.checksum.domain.SumsFormat;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/** State dan aksi tab Generate: daftar file → checksum → file {@code SHA256SUMS}. */
final class GenerateViewModel {

    private static final double NANOS_PER_SECOND = 1e9;

    private final ChecksumUseCases useCases;
    private final ToolContext context;
    private final TempFiles tempFiles;

    private final ObservableList<Path> files = FXCollections.observableArrayList();
    private final ObjectProperty<ChecksumAlgorithm> algorithm = new SimpleObjectProperty<>(ChecksumAlgorithm.DEFAULT);
    private final ObservableList<FileChecksum> results = FXCollections.observableArrayList();
    /** Algoritma yang dipakai untuk {@link #results}; menentukan nama file download. */
    private final ObjectProperty<ChecksumAlgorithm> resultAlgorithm = new SimpleObjectProperty<>();

    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final ObjectProperty<RunningTask> currentTask = new SimpleObjectProperty<>();
    private RunningTask runningTask;
    /** Satu temp file dipakai ulang supaya hasil berulang tidak menumpuk di disk. */
    private Path outputFile;

    GenerateViewModel(ChecksumUseCases useCases, ToolContext context, TempFiles tempFiles) {
        this.useCases = useCases;
        this.context = context;
        this.tempFiles = tempFiles;
        algorithm.addListener((observable, previous, current) -> clearResults());
    }

    ObservableList<Path> files() {
        return files;
    }

    ObjectProperty<ChecksumAlgorithm> algorithmProperty() {
        return algorithm;
    }

    ObservableList<FileChecksum> results() {
        return results;
    }

    ReadOnlyObjectProperty<RunningTask> currentTaskProperty() {
        return currentTask;
    }

    BooleanBinding canRun() {
        return busy.not().and(Bindings.isNotEmpty(files));
    }

    BooleanBinding hasResults() {
        return Bindings.isNotEmpty(results);
    }

    /** File yang sudah ada di daftar tidak ditambahkan dua kali. */
    void addFiles(List<Path> added) {
        added.stream().filter(file -> !files.contains(file)).forEach(files::add);
        clearResults();
    }

    void clearFiles() {
        files.clear();
        clearResults();
    }

    void run() {
        List<Path> inputs = List.copyOf(files);
        ChecksumAlgorithm chosen = algorithm.get();
        Path output = outputFile();
        clearResults();
        track(context.tasks()
                .submit(
                        (progress, cancellation) -> {
                            List<FileChecksum> checksums =
                                    useCases.generate().execute(inputs, chosen, progress, cancellation);
                            useCases.export().execute(checksums, output);
                            return checksums;
                        },
                        checksums -> {
                            results.setAll(checksums);
                            resultAlgorithm.set(chosen);
                        },
                        error -> ChecksumErrorMessages.show(context.owner(), error)));
    }

    /** Isi file SUMS, untuk Copy. */
    String sumsText() {
        return SumsFormat.write(results.stream().map(FileChecksum::toSumsEntry).toList());
    }

    void download() {
        if (!results.isEmpty()) {
            context.downloads().download(outputFile, resultAlgorithm.get().sumsFileName());
        }
    }

    void saveAs() {
        if (!results.isEmpty()) {
            context.downloads().saveAs(outputFile, resultAlgorithm.get().sumsFileName());
        }
    }

    String filesSummary() {
        if (files.isEmpty()) {
            return Messages.get("devutils.checksum.files.none");
        }
        return Messages.format("devutils.checksum.files.selected", files.size(), names());
    }

    String resultSummary() {
        long totalBytes = results.stream().mapToLong(FileChecksum::sizeBytes).sum();
        Duration totalDuration = results.stream().map(FileChecksum::duration).reduce(Duration.ZERO, Duration::plus);
        return Messages.format(
                "devutils.checksum.result.summary",
                results.size(),
                ByteSizeFormat.format(totalBytes),
                seconds(totalDuration));
    }

    static double seconds(Duration duration) {
        return duration.toNanos() / NANOS_PER_SECOND;
    }

    void dispose() {
        if (runningTask != null) {
            runningTask.cancel();
        }
        tempFiles.close();
    }

    private String names() {
        return String.join(
                ", ", files.stream().map(file -> file.getFileName().toString()).toList());
    }

    private Path outputFile() {
        if (outputFile == null) {
            outputFile = tempFiles.createFile(".txt");
        }
        return outputFile;
    }

    private void clearResults() {
        results.clear();
        resultAlgorithm.set(null);
    }

    private void track(RunningTask task) {
        runningTask = task;
        busy.set(true);
        task.runningProperty().addListener((observable, wasRunning, running) -> busy.set(running));
        currentTask.set(task);
    }
}
