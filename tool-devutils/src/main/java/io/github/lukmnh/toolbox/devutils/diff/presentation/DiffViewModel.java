package io.github.lukmnh.toolbox.devutils.diff.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.devutils.diff.domain.CompareRequest;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffOptions;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffResult;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffRow;
import io.github.lukmnh.toolbox.devutils.diff.domain.LoadedText;
import io.github.lukmnh.toolbox.devutils.diff.domain.WhitespaceMode;
import io.github.lukmnh.toolbox.devutils.shared.presentation.ClipboardActions;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/** State dan aksi Text Diff. */
final class DiffViewModel {

    /** Nama di header unified diff bila kedua sisi hasil paste. */
    static final String PASTED_FILE_NAME = "teks.txt";

    static final int NO_FOCUS = -1;
    private static final String DIFF_EXTENSION = ".diff";

    private final DiffUseCases useCases;
    private final ToolContext context;
    private final TempFiles tempFiles;

    private final DiffInput left = new DiffInput();
    private final DiffInput right = new DiffInput();
    private final ObjectProperty<WhitespaceMode> whitespace = new SimpleObjectProperty<>(WhitespaceMode.EXACT);
    private final BooleanProperty ignoreCase = new SimpleBooleanProperty();
    private final BooleanProperty normalizeLineEndings = new SimpleBooleanProperty(true);
    private final ObjectProperty<DiffViewMode> viewMode = new SimpleObjectProperty<>(DiffViewMode.SIDE_BY_SIDE);

    private final ObjectProperty<DiffResult> result = new SimpleObjectProperty<>();
    private final ObservableList<DiffRow> rows = FXCollections.observableArrayList();
    private final ObservableList<UnifiedLine> unifiedLines = FXCollections.observableArrayList();
    private final List<Integer> unifiedHunkStarts = new ArrayList<>();
    private final BooleanProperty stale = new SimpleBooleanProperty();
    private final IntegerProperty focusIndex = new SimpleIntegerProperty(NO_FOCUS);
    private int changeCursor = NO_FOCUS;

    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final ObjectProperty<RunningTask> currentTask = new SimpleObjectProperty<>();
    private RunningTask runningTask;
    /** Satu temp file dipakai ulang supaya export berulang tidak menumpuk di disk. */
    private Path outputFile;

    DiffViewModel(DiffUseCases useCases, ToolContext context, TempFiles tempFiles) {
        this.useCases = useCases;
        this.context = context;
        this.tempFiles = tempFiles;
        markStaleOnChange(
                left.textProperty(),
                left.largeFileProperty(),
                right.textProperty(),
                right.largeFileProperty(),
                whitespace,
                ignoreCase,
                normalizeLineEndings);
        viewMode.addListener((observable, previous, current) -> changeCursor = NO_FOCUS);
    }

    DiffInput left() {
        return left;
    }

    DiffInput right() {
        return right;
    }

    ObjectProperty<WhitespaceMode> whitespaceProperty() {
        return whitespace;
    }

    BooleanProperty ignoreCaseProperty() {
        return ignoreCase;
    }

    BooleanProperty normalizeLineEndingsProperty() {
        return normalizeLineEndings;
    }

    ObjectProperty<DiffViewMode> viewModeProperty() {
        return viewMode;
    }

    ReadOnlyObjectProperty<DiffResult> resultProperty() {
        return result;
    }

    ObservableList<DiffRow> rows() {
        return rows;
    }

    ObservableList<UnifiedLine> unifiedLines() {
        return unifiedLines;
    }

    ReadOnlyBooleanProperty staleProperty() {
        return stale;
    }

    /** Indeks baris (di list mode aktif) yang harus di-scroll; berubah setiap Prev/Next. */
    ReadOnlyIntegerProperty focusIndexProperty() {
        return focusIndex;
    }

    ReadOnlyObjectProperty<RunningTask> currentTaskProperty() {
        return currentTask;
    }

    BooleanBinding canCompare() {
        return busy.not().and(left.isEmpty().and(right.isEmpty()).not());
    }

    BooleanBinding hasChanges() {
        return Bindings.createBooleanBinding(
                () -> result.get() != null && !result.get().stats().identical(), result);
    }

    BooleanBinding canExport() {
        return hasChanges().and(busy.not());
    }

    void compare() {
        CompareRequest request =
                new CompareRequest(left.effectiveText(), right.effectiveText(), headerFileName(), options());
        track(context.tasks()
                .submit(
                        (progress, cancellation) -> useCases.compare().execute(request, cancellation),
                        this::showResult,
                        error -> DiffErrorMessages.show(context.owner(), error)));
    }

    void loadFile(DiffInput side, Path file) {
        track(context.tasks()
                .submit(
                        (progress, cancellation) -> useCases.load().execute(file),
                        side::load,
                        error -> DiffErrorMessages.show(context.owner(), error)));
    }

    void swap() {
        left.swapWith(right);
    }

    void next() {
        moveCursor(1);
    }

    void previous() {
        moveCursor(-1);
    }

    void copyUnified() {
        if (result.get() != null) {
            ClipboardActions.copy(result.get().unified().text(), context.notifier());
        }
    }

    void download() {
        export((file, name) -> context.downloads().download(file, name));
    }

    void saveAs() {
        export((file, name) -> context.downloads().saveAs(file, name));
    }

    String fileSummary(DiffInput side) {
        LoadedText large = side.largeFileProperty().get();
        if (large != null) {
            return Messages.format(
                    "devutils.diff.input.largeFile", large.fileName(), ByteSizeFormat.format(large.sizeBytes()));
        }
        String name = side.fileNameProperty().get();
        return name.isEmpty()
                ? Messages.get("devutils.diff.input.pasted")
                : Messages.format("devutils.diff.input.file", name);
    }

    void dispose() {
        if (runningTask != null) {
            runningTask.cancel();
        }
        tempFiles.close();
    }

    private DiffOptions options() {
        return new DiffOptions(whitespace.get(), ignoreCase.get(), normalizeLineEndings.get());
    }

    /** Nama file kiri (asli) dipakai untuk kedua header supaya {@code git apply} memperlakukannya sebagai modifikasi. */
    private String headerFileName() {
        String leftName = left.fileNameProperty().get();
        if (!leftName.isEmpty()) {
            return leftName;
        }
        String rightName = right.fileNameProperty().get();
        return rightName.isEmpty() ? PASTED_FILE_NAME : rightName;
    }

    private void showResult(DiffResult compared) {
        result.set(compared);
        rows.setAll(compared.rows());
        unifiedLines.setAll(UnifiedLine.classify(compared.unified().lines()));
        unifiedHunkStarts.clear();
        for (int index = 0; index < unifiedLines.size(); index++) {
            if (unifiedLines.get(index).kind() == UnifiedLine.Kind.HUNK) {
                unifiedHunkStarts.add(index);
            }
        }
        stale.set(false);
        changeCursor = NO_FOCUS;
        focusIndex.set(NO_FOCUS);
    }

    /** Berputar: setelah perubahan terakhir kembali ke yang pertama. */
    private void moveCursor(int step) {
        List<Integer> starts = viewMode.get() == DiffViewMode.SIDE_BY_SIDE
                ? (result.get() == null ? List.of() : result.get().changeStarts())
                : unifiedHunkStarts;
        if (starts.isEmpty()) {
            return;
        }
        changeCursor = changeCursor == NO_FOCUS && step < 0
                ? starts.size() - 1
                : Math.floorMod(changeCursor + step, starts.size());
        // Reset dulu supaya listener tetap terpanggil walau indeksnya sama dengan sebelumnya
        focusIndex.set(NO_FOCUS);
        focusIndex.set(starts.get(changeCursor));
    }

    private void export(BiConsumer<Path, String> deliver) {
        DiffResult current = result.get();
        if (current == null || current.unified().isEmpty()) {
            return;
        }
        String name = baseName(headerFileName()) + DIFF_EXTENSION;
        Path target = outputFile();
        track(context.tasks()
                .submit(
                        (progress, cancellation) -> {
                            useCases.export().execute(current.unified(), target);
                            return target;
                        },
                        file -> deliver.accept(file, name),
                        error -> DiffErrorMessages.show(context.owner(), error)));
    }

    private Path outputFile() {
        if (outputFile == null) {
            outputFile = tempFiles.createFile(DIFF_EXTENSION);
        }
        return outputFile;
    }

    private static String baseName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot <= 0 ? fileName : fileName.substring(0, dot);
    }

    private void markStaleOnChange(Observable... sources) {
        for (Observable source : sources) {
            source.addListener(observable -> stale.set(result.get() != null));
        }
    }

    private void track(RunningTask task) {
        runningTask = task;
        busy.set(true);
        task.runningProperty().addListener((observable, wasRunning, running) -> busy.set(running));
        currentTask.set(task);
    }
}
