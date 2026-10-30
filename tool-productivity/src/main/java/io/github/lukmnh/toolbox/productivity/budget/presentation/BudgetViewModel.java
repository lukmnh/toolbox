package io.github.lukmnh.toolbox.productivity.budget.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.Debounce;
import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetEntry;
import io.github.lukmnh.toolbox.productivity.budget.domain.OpenedBudget;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
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
import javafx.collections.ObservableList;
import javafx.util.Duration;

/**
 * State Budget: daftar budget, budget yang terbuka, dan autosave.
 *
 * <p>Autosave: setiap perubahan menaikkan {@link #revision}; 1 detik setelah perubahan terakhir state disimpan di
 * background. {@link WriteSequencer} menjamin penulisan lama tidak menimpa yang baru. Operasi struktural (ganti
 * budget, rename, hapus, …) menyimpan perubahan tertunda di task yang sama, dan editor dikunci selama berjalan.
 * Semua method dipanggil dari FX thread.
 */
final class BudgetViewModel {

    private static final System.Logger LOG = System.getLogger(BudgetViewModel.class.getName());
    private static final Duration AUTOSAVE_DELAY = Duration.seconds(1);
    private static final String JSON_EXTENSION = ".json";
    private static final String XLSX_EXTENSION = ".xlsx";

    private final BudgetUseCases useCases;
    private final ToolContext context;
    private final TempFiles tempFiles;
    private final WriteSequencer writes = new WriteSequencer();
    private final Debounce autosave = new Debounce(AUTOSAVE_DELAY, this::autosaveNow);

    private final ObservableList<BudgetEntry> entries = FXCollections.observableArrayList();
    private final ObjectProperty<BudgetEntry> currentEntry = new SimpleObjectProperty<>();
    private final ObjectProperty<Budget> budget = new SimpleObjectProperty<>();
    private final ObjectProperty<SaveState> saveState = new SimpleObjectProperty<>(SaveState.SAVED);
    private final StringProperty saveError = new SimpleStringProperty("");
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final List<RunningTask> exports = new ArrayList<>();

    /** Revisi state di layar; naik setiap ada perubahan dari user. */
    private long revision;
    /** Revisi terakhir yang sudah dipastikan ada di disk. */
    private long savedRevision;

    BudgetViewModel(BudgetUseCases useCases, ToolContext context, TempFiles tempFiles) {
        this.useCases = useCases;
        this.context = context;
        this.tempFiles = tempFiles;
    }

    ObservableList<BudgetEntry> entries() {
        return entries;
    }

    ReadOnlyObjectProperty<BudgetEntry> currentEntryProperty() {
        return currentEntry;
    }

    /** {@code null} bila belum ada budget yang terbuka. */
    ReadOnlyObjectProperty<Budget> budgetProperty() {
        return budget;
    }

    ReadOnlyObjectProperty<SaveState> saveStateProperty() {
        return saveState;
    }

    ReadOnlyStringProperty saveErrorProperty() {
        return saveError;
    }

    ReadOnlyBooleanProperty busyProperty() {
        return busy;
    }

    ToolContext context() {
        return context;
    }

    /** Muat daftar budget lalu buka yang pertama. */
    void start() {
        runStructural(false, () -> useCases.list().execute(), this::showListAndOpenFirst);
    }

    void select(BudgetEntry entry) {
        BudgetEntry current = currentEntry.get();
        if (busy.get() || (current != null && current.id().equals(entry.id()))) {
            return;
        }
        List<BudgetEntry> list = List.copyOf(entries);
        runStructural(
                true,
                () -> new Refreshed(
                        list, new OpenedBudget(entry, useCases.load().execute(entry.id()))),
                this::showRefreshed);
    }

    void create(String name) {
        String firstSheet = Messages.format("productivity.budget.sheet.defaultName", 1);
        runStructural(true, () -> refreshed(useCases.create().execute(name, firstSheet)), this::showRefreshed);
    }

    void renameCurrent(String name) {
        BudgetEntry entry = currentEntry.get();
        Budget snapshot = budget.get();
        if (entry == null || snapshot == null) {
            return;
        }
        // Rename menulis state terbaru ke file baru, jadi perubahan tertunda tidak perlu disimpan terpisah
        runStructural(
                false, () -> refreshed(useCases.rename().execute(entry.id(), snapshot, name)), this::showRefreshed);
    }

    void duplicateCurrent() {
        Budget snapshot = budget.get();
        if (snapshot != null) {
            runStructural(true, () -> refreshed(useCases.duplicate().execute(snapshot)), this::showRefreshed);
        }
    }

    void deleteCurrent() {
        BudgetEntry entry = currentEntry.get();
        if (entry == null) {
            return;
        }
        autosave.stop();
        runStructural(
                false,
                () -> {
                    useCases.delete().execute(entry.id());
                    return useCases.list().execute();
                },
                list -> {
                    close();
                    showListAndOpenFirst(list);
                });
    }

    void importJson(Path source) {
        runStructural(true, () -> refreshed(useCases.importJson().execute(source)), opened -> {
            showRefreshed(opened);
            String name = opened.opened().budget().name();
            context.notifier().info(Messages.format("productivity.budget.import.done", name));
        });
    }

    void exportJson() {
        export(JSON_EXTENSION, (snapshot, target) -> useCases.exportJson().execute(snapshot, target));
    }

    void exportWorkbook() {
        export(
                XLSX_EXTENSION,
                (snapshot, target) -> useCases.exportWorkbook().execute(snapshot, BudgetLabels.workbook(), target));
    }

    /** Perubahan dari user. Nilai tidak valid dari domain (misalnya nama kosong) ditolak dengan peringatan. */
    void edit(UnaryOperator<Budget> change) {
        Budget current = budget.get();
        if (current == null || busy.get()) {
            return;
        }
        Budget changed;
        try {
            changed = change.apply(current);
        } catch (IllegalArgumentException e) {
            context.notifier().warning(Messages.get("productivity.budget.edit.invalid"));
            return;
        }
        if (changed.equals(current)) {
            return;
        }
        budget.set(changed);
        revision++;
        saveState.set(SaveState.SAVING);
        autosave.trigger();
    }

    /** Simpan perubahan tertunda secara sinkron lalu batalkan export. Dipanggil dari {@code Tool.dispose()}. */
    void dispose() {
        autosave.stop();
        flushNow();
        exports.forEach(RunningTask::cancel);
        exports.clear();
        tempFiles.close();
    }

    private boolean dirty() {
        return revision > savedRevision;
    }

    private void autosaveNow() {
        BudgetEntry entry = currentEntry.get();
        if (entry == null || !dirty()) {
            return;
        }
        if (busy.get()) {
            // Operasi struktural sedang berjalan dan ikut menyimpan; coba lagi setelahnya bila masih ada sisa
            autosave.trigger();
            return;
        }
        long target = revision;
        Budget snapshot = budget.get();
        context.tasks()
                .submit(
                        (progress, cancellation) -> writes.writeIfNewer(
                                target, () -> useCases.save().execute(entry.id(), snapshot)),
                        written -> markSaved(target),
                        this::markSaveFailed);
    }

    /** Penulisan sinkron terakhir: aman karena file budget kecil, dan wajib selesai sebelum tool ditutup. */
    private void flushNow() {
        BudgetEntry entry = currentEntry.get();
        Budget snapshot = budget.get();
        if (entry == null || snapshot == null || !dirty()) {
            return;
        }
        try {
            writes.writeIfNewer(revision, () -> useCases.save().execute(entry.id(), snapshot));
            savedRevision = revision;
        } catch (RuntimeException e) {
            // Tidak ada UI lagi saat dispose; isi budget tidak dicatat, hanya jenis error (Security Requirements #5)
            LOG.log(
                    Level.ERROR,
                    "Autosave terakhir budget gagal: " + e.getClass().getSimpleName());
        }
    }

    private void markSaved(long target) {
        savedRevision = Math.max(savedRevision, target);
        if (!dirty()) {
            saveState.set(SaveState.SAVED);
            saveError.set("");
        }
    }

    private void markSaveFailed(Throwable error) {
        saveState.set(SaveState.FAILED);
        saveError.set(BudgetErrorMessages.describe(error));
        LOG.log(Level.WARNING, "Autosave budget gagal: " + error.getClass().getSimpleName());
    }

    /**
     * Jalankan operasi file yang mengubah daftar/budget terbuka. Editor dikunci ({@link #busy}) sampai selesai.
     *
     * @param flushFirst simpan perubahan tertunda budget saat ini dulu, di task yang sama
     */
    private <T> void runStructural(boolean flushFirst, Supplier<T> operation, Consumer<T> onSuccess) {
        autosave.stop();
        long target = revision;
        BudgetEntry entry = currentEntry.get();
        Budget snapshot = budget.get();
        boolean save = flushFirst && entry != null && snapshot != null && dirty();
        busy.set(true);
        context.tasks()
                .submit(
                        (progress, cancellation) -> writes.exclusive(target, () -> {
                            if (save) {
                                useCases.save().execute(entry.id(), snapshot);
                            }
                            return operation.get();
                        }),
                        result -> {
                            busy.set(false);
                            if (save) {
                                markSaved(target);
                            }
                            onSuccess.accept(result);
                        },
                        error -> {
                            busy.set(false);
                            if (dirty()) {
                                autosave.trigger();
                            }
                            BudgetErrorMessages.show(context.owner(), error);
                        });
    }

    private Refreshed refreshed(OpenedBudget opened) {
        return new Refreshed(useCases.list().execute(), opened);
    }

    private void showRefreshed(Refreshed refreshed) {
        entries.setAll(refreshed.entries());
        open(refreshed.opened());
    }

    private void showListAndOpenFirst(List<BudgetEntry> list) {
        entries.setAll(list);
        if (!list.isEmpty()) {
            select(list.getFirst());
        }
    }

    private void open(OpenedBudget opened) {
        revision++;
        savedRevision = revision;
        currentEntry.set(opened.entry());
        budget.set(opened.budget());
        saveState.set(SaveState.SAVED);
        saveError.set("");
    }

    private void close() {
        revision++;
        savedRevision = revision;
        currentEntry.set(null);
        budget.set(null);
        saveState.set(SaveState.SAVED);
    }

    private void export(String extension, ExportWork work) {
        Budget snapshot = budget.get();
        if (snapshot == null) {
            return;
        }
        Path target = tempFiles.createFile(extension);
        RunningTask task = context.tasks()
                .submit(
                        (progress, cancellation) -> {
                            work.write(snapshot, target);
                            return target;
                        },
                        file -> context.downloads().download(file, snapshot.name() + extension),
                        error -> BudgetErrorMessages.show(context.owner(), error));
        exports.removeIf(running -> !running.runningProperty().get());
        exports.add(task);
    }

    @FunctionalInterface
    private interface ExportWork {
        void write(Budget snapshot, Path target);
    }

    /** Hasil operasi struktural: daftar terbaru + budget yang dibuka. */
    private record Refreshed(List<BudgetEntry> entries, OpenedBudget opened) {}
}
