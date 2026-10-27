package io.github.lukmnh.toolbox.pdf.merge.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.pdf.merge.application.MergePdfUseCase;
import io.github.lukmnh.toolbox.pdf.merge.domain.MergeRequest;
import io.github.lukmnh.toolbox.pdf.merge.domain.MergeResult;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import io.github.lukmnh.toolbox.pdf.shared.presentation.PdfFailureHandler;
import java.nio.file.Path;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/** State dan aksi tool Gabung PDF. */
public final class MergeViewModel {

    private static final String OUTPUT_NAME = "merged.pdf";

    private final MergePdfUseCase mergeUseCase;
    private final ToolContext context;
    private final TempFiles tempFiles;

    private final ObservableList<PdfSource> files = FXCollections.observableArrayList();
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final ObjectProperty<MergeResult> result = new SimpleObjectProperty<>();
    private final ObjectProperty<RunningTask> currentTask = new SimpleObjectProperty<>();
    private RunningTask runningTask;

    public MergeViewModel(MergePdfUseCase mergeUseCase, ToolContext context, TempFiles tempFiles) {
        this.mergeUseCase = mergeUseCase;
        this.context = context;
        this.tempFiles = tempFiles;
        // Hasil lama tidak berlaku lagi begitu daftar atau urutan file berubah
        files.addListener((ListChangeListener<PdfSource>) change -> result.set(null));
    }

    public ObservableList<PdfSource> files() {
        return files;
    }

    public BooleanProperty busyProperty() {
        return busy;
    }

    public ReadOnlyObjectProperty<MergeResult> resultProperty() {
        return result;
    }

    public ReadOnlyObjectProperty<RunningTask> currentTaskProperty() {
        return currentTask;
    }

    public BooleanBinding canMerge() {
        return Bindings.size(files).lessThan(MergeRequest.MIN_SOURCES).or(busy).not();
    }

    public void addFiles(List<Path> paths) {
        paths.stream()
                .filter(path -> files.stream().noneMatch(source -> source.file().equals(path)))
                .map(PdfSource::of)
                .forEach(files::add);
    }

    public void remove(int index) {
        if (index >= 0 && index < files.size()) {
            files.remove(index);
        }
    }

    /** Pindahkan item; dipakai oleh tombol Naik/Turun dan drag-reorder. */
    public void move(int from, int to) {
        if (from < 0 || from >= files.size() || to < 0 || to >= files.size() || from == to) {
            return;
        }
        PdfSource moved = files.remove(from);
        files.add(to, moved);
    }

    public void merge() {
        List<PdfSource> sources = List.copyOf(files);
        Path output = tempFiles.createFile(".pdf");
        busy.set(true);
        runningTask = context.tasks()
                .submit(
                        (progress, cancellation) ->
                                mergeUseCase.execute(new MergeRequest(sources, output), progress, cancellation),
                        result::set,
                        error -> PdfFailureHandler.handle(context.owner(), error, this::retryWithPassword));
        // Juga menangani pembatalan, yang tidak memanggil callback mana pun
        runningTask.runningProperty().addListener((observable, wasRunning, running) -> busy.set(running));
        currentTask.set(runningTask);
    }

    private void retryWithPassword(Path file, String password) {
        files.replaceAll(source -> source.file().equals(file) ? source.withPassword(password) : source);
        merge();
    }

    public void download() {
        if (result.get() != null) {
            context.downloads().download(result.get().output(), OUTPUT_NAME);
        }
    }

    public void saveAs() {
        if (result.get() != null) {
            context.downloads().saveAs(result.get().output(), OUTPUT_NAME);
        }
    }

    public String resultSummary(MergeResult merged) {
        return Messages.format("pdf.merge.done", merged.fileCount(), merged.pageCount());
    }

    public void dispose() {
        if (runningTask != null) {
            runningTask.cancel();
        }
        files.clear();
        tempFiles.close();
    }
}
