package io.github.lukmnh.toolbox.productivity.rename.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.task.BackgroundWork;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.Debounce;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.productivity.rename.application.RenameResult;
import io.github.lukmnh.toolbox.productivity.rename.application.UndoResult;
import io.github.lukmnh.toolbox.productivity.rename.domain.FileBatch;
import io.github.lukmnh.toolbox.productivity.rename.domain.FileEntry;
import io.github.lukmnh.toolbox.productivity.rename.domain.FileOrder;
import io.github.lukmnh.toolbox.productivity.rename.domain.PreviewRow;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameException;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenamePair;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenamePlan;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenamePreview;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameRule;
import io.github.lukmnh.toolbox.productivity.rename.domain.StoredRename;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.util.Duration;

/** State dan aksi Batch Rename: batch file, aturan, preview (debounce, background), eksekusi, dan undo. */
final class RenameViewModel {

    private static final Duration PREVIEW_DELAY = Duration.millis(250);

    private final RenameUseCases useCases;
    private final ToolContext context;
    private final Debounce previewDebounce;

    private final ObservableList<FileEntry> files = FXCollections.observableArrayList();
    private final ObjectProperty<Path> folder = new SimpleObjectProperty<>();
    private final ObjectProperty<FileOrder> order = new SimpleObjectProperty<>(FileOrder.NAME);
    private final ObservableList<RuleItem> rules =
            FXCollections.observableArrayList(item -> new Observable[] {item.enabledProperty(), item.ruleProperty()});

    /** {@code null} selama preview belum sesuai input terbaru (sedang dihitung atau error): eksekusi terkunci. */
    private final ObjectProperty<RenamePreview> preview = new SimpleObjectProperty<>();
    /** Baris preview terakhir per nama lama; dipertahankan saat menghitung ulang supaya tabel tidak berkedip. */
    private final ObjectProperty<Map<String, PreviewRow>> rowsByName = new SimpleObjectProperty<>(Map.of());

    private final StringProperty previewError = new SimpleStringProperty("");
    private final StringProperty historyWarning = new SimpleStringProperty("");
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final ObjectProperty<StoredRename> lastRename = new SimpleObjectProperty<>();
    private final ObjectProperty<RunningTask> currentTask = new SimpleObjectProperty<>();

    private RunningTask previewTask;
    private RunningTask mainTask;
    private RunningTask historyTask;

    RenameViewModel(RenameUseCases useCases, ToolContext context) {
        this.useCases = useCases;
        this.context = context;
        this.previewDebounce = new Debounce(PREVIEW_DELAY, this::runPreview);
        files.addListener((ListChangeListener<FileEntry>) change -> invalidatePreview());
        rules.addListener((ListChangeListener<RuleItem>) change -> invalidatePreview());
        order.addListener((observable, previous, current) -> files.setAll(current.sort(files)));
        refreshLastRename();
    }

    ObservableList<FileEntry> files() {
        return files;
    }

    ReadOnlyObjectProperty<Path> folderProperty() {
        return folder;
    }

    ObjectProperty<FileOrder> orderProperty() {
        return order;
    }

    ObservableList<RuleItem> rules() {
        return rules;
    }

    ReadOnlyObjectProperty<RenamePreview> previewProperty() {
        return preview;
    }

    ReadOnlyObjectProperty<Map<String, PreviewRow>> rowsByNameProperty() {
        return rowsByName;
    }

    ReadOnlyStringProperty previewErrorProperty() {
        return previewError;
    }

    ReadOnlyStringProperty historyWarningProperty() {
        return historyWarning;
    }

    ReadOnlyBooleanProperty busyProperty() {
        return busy;
    }

    ReadOnlyObjectProperty<StoredRename> lastRenameProperty() {
        return lastRename;
    }

    ReadOnlyObjectProperty<RunningTask> currentTaskProperty() {
        return currentTask;
    }

    Optional<PreviewRow> rowFor(FileEntry file) {
        return Optional.ofNullable(rowsByName.get().get(file.name()));
    }

    BooleanBinding canExecute() {
        return Bindings.createBooleanBinding(
                () -> !busy.get() && preview.get() != null && preview.get().executable(), busy, preview);
    }

    BooleanBinding canUndo() {
        return busy.not().and(lastRename.isNotNull());
    }

    /** Aturan ubah ekstensi yang aktif selalu diberi peringatan. */
    BooleanBinding changesExtension() {
        return Bindings.createBooleanBinding(this::hasActiveExtensionRule, rules);
    }

    /** Satu folder, atau file-file dari satu folder. */
    void load(List<Path> selection) {
        runMain((progress, cancellation) -> useCases.load().execute(selection, cancellation), this::showBatch);
    }

    void clearFiles() {
        files.clear();
        folder.set(null);
    }

    /** Pindah posisi file; urutan otomatis menjadi manual. */
    void moveFile(int from, int to) {
        if (from == to || from < 0 || to < 0 || from >= files.size() || to >= files.size()) {
            return;
        }
        order.set(FileOrder.MANUAL);
        FileEntry moved = files.remove(from);
        files.add(to, moved);
    }

    RuleItem addRule(RuleKind kind) {
        RuleItem item = new RuleItem(kind.defaultRule());
        rules.add(item);
        return item;
    }

    void removeRule(RuleItem item) {
        rules.remove(item);
    }

    void moveRule(int from, int to) {
        if (from == to || from < 0 || to < 0 || from >= rules.size() || to >= rules.size()) {
            return;
        }
        RuleItem moved = rules.remove(from);
        rules.add(to, moved);
    }

    void execute() {
        RenamePreview current = preview.get();
        if (busy.get() || current == null || !current.executable() || !confirmExecute(current)) {
            return;
        }
        RenamePlan plan = new RenamePlan(current.folder(), current.changes());
        runMain((progress, cancellation) -> useCases.execute().execute(plan, progress, cancellation), this::onRenamed);
    }

    void undo() {
        StoredRename last = lastRename.get();
        if (busy.get() || last == null || !confirmUndo(last)) {
            return;
        }
        runMain((progress, cancellation) -> useCases.undo().execute(progress, cancellation), this::onUndone);
    }

    /** Membatalkan rename yang sedang berjalan memicu rollback ke nama asal. */
    void dispose() {
        previewDebounce.stop();
        for (RunningTask task : new RunningTask[] {previewTask, mainTask, historyTask}) {
            if (task != null) {
                task.cancel();
            }
        }
    }

    private void showBatch(FileBatch batch) {
        folder.set(batch.folder());
        files.setAll(order.get().sort(batch.files()));
    }

    private void invalidatePreview() {
        preview.set(null);
        previewDebounce.trigger();
    }

    private void runPreview() {
        if (previewTask != null) {
            previewTask.cancel();
        }
        if (files.isEmpty() || folder.get() == null) {
            rowsByName.set(Map.of());
            previewError.set("");
            return;
        }
        FileBatch batch = new FileBatch(folder.get(), List.copyOf(files));
        List<RenameRule> active =
                rules.stream().filter(RuleItem::enabled).map(RuleItem::rule).toList();
        previewTask = context.tasks()
                .submit(
                        (progress, cancellation) -> useCases.preview().execute(batch, active, cancellation),
                        this::showPreview,
                        this::showPreviewError);
    }

    private void showPreview(RenamePreview result) {
        previewError.set("");
        rowsByName.set(result.rows().stream()
                .collect(Collectors.toUnmodifiableMap(row -> row.file().name(), row -> row)));
        preview.set(result);
    }

    /** Error aturan (regex salah, dsb.) tampil inline karena muncul berulang saat user mengetik. */
    private void showPreviewError(Throwable error) {
        if (error instanceof RenameException renameError) {
            previewError.set(RenameErrorMessages.describe(renameError));
            return;
        }
        previewError.set("");
        ErrorDialogs.showUnexpected(context.owner(), error);
    }

    private void onRenamed(RenameResult result) {
        context.notifier()
                .success(
                        Messages.format(
                                "productivity.rename.done", result.pairs().size()),
                        List.of());
        if (!result.historySaved()) {
            context.notifier().warning(Messages.get("productivity.rename.history.notSaved"));
        }
        reloadRenamed(result.pairs());
        refreshLastRename();
    }

    private void onUndone(UndoResult result) {
        context.notifier()
                .success(
                        Messages.format(
                                "productivity.rename.undo.done",
                                result.undone().log().pairs().size()),
                        List.of());
        if (!result.logRemoved()) {
            context.notifier().warning(Messages.get("productivity.rename.history.notRemoved"));
        }
        if (result.undone().log().folder().equals(folder.get())) {
            reloadRenamed(result.undone().log().pairs().stream()
                    .map(RenamePair::reversed)
                    .toList());
        }
        refreshLastRename();
    }

    /** Muat ulang batch dengan nama baru, urutan dipertahankan. */
    private void reloadRenamed(List<RenamePair> pairs) {
        if (folder.get() == null || files.isEmpty()) {
            return;
        }
        Map<String, String> renamed = pairs.stream().collect(Collectors.toMap(RenamePair::from, RenamePair::to));
        Path base = folder.get();
        List<Path> selection = files.stream()
                .map(file -> base.resolve(renamed.getOrDefault(file.name(), file.name())))
                .toList();
        load(selection);
    }

    private void refreshLastRename() {
        historyTask = context.tasks()
                .submit(
                        (progress, cancellation) -> useCases.findLast().execute(),
                        found -> {
                            historyWarning.set("");
                            lastRename.set(found.orElse(null));
                        },
                        this::showHistoryError);
    }

    private void showHistoryError(Throwable error) {
        lastRename.set(null);
        if (error instanceof RenameException renameError) {
            historyWarning.set(RenameErrorMessages.describe(renameError));
            return;
        }
        ErrorDialogs.showUnexpected(context.owner(), error);
    }

    private boolean confirmExecute(RenamePreview current) {
        int count = current.changes().size();
        String detail = Messages.format("productivity.rename.confirm.detail", count);
        if (hasActiveExtensionRule()) {
            detail = detail + "\n\n" + Messages.get("productivity.rename.extension.warning");
        }
        return ErrorDialogs.confirm(
                context.owner(),
                Messages.format("productivity.rename.confirm.header", count),
                detail,
                Messages.get("productivity.rename.execute"));
    }

    private boolean confirmUndo(StoredRename last) {
        Path lastFolder = last.log().folder();
        return ErrorDialogs.confirm(
                context.owner(),
                Messages.get("productivity.rename.undo.confirm.header"),
                Messages.format(
                        "productivity.rename.undo.confirm.detail",
                        last.log().pairs().size(),
                        String.valueOf(lastFolder.getFileName())),
                Messages.get("productivity.rename.undo"));
    }

    private boolean hasActiveExtensionRule() {
        return rules.stream().anyMatch(item -> item.enabled() && item.rule() instanceof RenameRule.ChangeExtension);
    }

    private <T> void runMain(BackgroundWork<T> work, Consumer<T> onSuccess) {
        busy.set(true);
        RunningTask task = context.tasks().submit(work, onSuccess::accept, error -> {
            // Folder bisa berubah (misalnya rollback sebagian), jadi preview lama tidak boleh dipakai lagi
            invalidatePreview();
            RenameErrorMessages.show(context.owner(), error);
        });
        task.runningProperty().addListener((observable, wasRunning, running) -> busy.set(running));
        mainTask = task;
        currentTask.set(task);
    }
}
