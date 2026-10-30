package io.github.lukmnh.toolbox.json.viewer.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.FileExtensions;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.task.BackgroundWork;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.json.shared.domain.InvalidQueryException;
import io.github.lukmnh.toolbox.json.shared.domain.JsonToolException;
import io.github.lukmnh.toolbox.json.shared.presentation.JsonErrorMessages;
import io.github.lukmnh.toolbox.json.viewer.application.ViewerSession;
import io.github.lukmnh.toolbox.json.viewer.domain.ExportFormat;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonNodeInfo;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonWriter;
import io.github.lukmnh.toolbox.json.viewer.domain.NodeChildren;
import io.github.lukmnh.toolbox.json.viewer.domain.RecordColumn;
import io.github.lukmnh.toolbox.json.viewer.domain.SortSpec;
import io.github.lukmnh.toolbox.json.viewer.domain.ViewFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;

/** State dan aksi JSON Viewer. Semua method dipanggil dari FX thread; proses berat lewat TaskRunner. */
public final class ViewerViewModel {

    public static final Set<String> EXTENSIONS = Set.of("json", "jsonl", "ndjson");

    /** Preview nilai nested di sel tabel (BR-JS-6). */
    private static final int CELL_PREVIEW_MAX_CHARS = 200;
    /** Panel detail menampilkan maksimal 1 MB pertama record. */
    private static final int DETAIL_MAX_BYTES = 1024 * 1024;

    private static final Duration MEMORY_REFRESH = Duration.seconds(2);
    private static final double NANOS_PER_SECOND = 1e9;

    private final ViewerUseCases useCases;
    private final ToolContext context;
    private final TempFiles tempFiles;

    private final ObjectProperty<ViewerSession> session = new SimpleObjectProperty<>();
    /** View yang tampil: hasil filter lalu sort. */
    private final ObjectProperty<int[]> rows = new SimpleObjectProperty<>();
    /** Hasil filter sebelum sort, supaya sort bisa diganti/dihapus tanpa memfilter ulang. */
    private int[] filteredRows = new int[0];

    private final ObjectProperty<SortSpec> sortSpec = new SimpleObjectProperty<>();
    private final StringProperty quickSearch = new SimpleStringProperty("");
    private final StringProperty query = new SimpleStringProperty("");
    private final ObjectProperty<InvalidQueryException> queryError = new SimpleObjectProperty<>();
    private final IntegerProperty selectedRecord = new SimpleIntegerProperty(-1);
    private final ObjectProperty<ExportFormat> exportFormat = new SimpleObjectProperty<>(ExportFormat.JSON_ARRAY);
    private final ObservableList<Path> recentFiles = FXCollections.observableArrayList();
    private final StringProperty memory = new SimpleStringProperty("");
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final ObjectProperty<RunningTask> currentTask = new SimpleObjectProperty<>();
    private final List<RunningTask> backgroundTasks = new ArrayList<>();
    private final Map<ExportFormat, Path> exportFiles = new EnumMap<>(ExportFormat.class);
    private final Timeline memoryTimer;
    private RunningTask runningTask;
    private boolean disposed;

    public ViewerViewModel(ViewerUseCases useCases, ToolContext context, TempFiles tempFiles) {
        this.useCases = useCases;
        this.context = context;
        this.tempFiles = tempFiles;
        memoryTimer = new Timeline(new KeyFrame(MEMORY_REFRESH, event -> refreshMemory()));
        memoryTimer.setCycleCount(Timeline.INDEFINITE);
        memoryTimer.play();
        refreshMemory();
        background((progress, cancellation) -> useCases.recent().list(), recentFiles::setAll);
    }

    public ReadOnlyObjectProperty<ViewerSession> sessionProperty() {
        return session;
    }

    public ReadOnlyObjectProperty<int[]> rowsProperty() {
        return rows;
    }

    public StringProperty quickSearchProperty() {
        return quickSearch;
    }

    public StringProperty queryProperty() {
        return query;
    }

    public ReadOnlyObjectProperty<InvalidQueryException> queryErrorProperty() {
        return queryError;
    }

    public ReadOnlyIntegerProperty selectedRecordProperty() {
        return selectedRecord;
    }

    public ObjectProperty<ExportFormat> exportFormatProperty() {
        return exportFormat;
    }

    public ObservableList<Path> recentFiles() {
        return recentFiles;
    }

    public ReadOnlyObjectProperty<RunningTask> currentTaskProperty() {
        return currentTask;
    }

    public BooleanBinding tableMode() {
        return Bindings.createBooleanBinding(
                () -> session.get() != null && session.get().format().isTable(), session);
    }

    public BooleanBinding treeMode() {
        return Bindings.createBooleanBinding(
                () -> session.get() != null && !session.get().format().isTable(), session);
    }

    /** Filter, sort, dan export hanya di mode tabel dan saat tidak ada proses berjalan. */
    public BooleanBinding canQuery() {
        return tableMode().and(busy.not());
    }

    public void openFile(Path file) {
        cancelAll();
        track((progress, cancellation) -> useCases.open().execute(file, progress, cancellation), opened -> {
            ViewerSession previous = session.get();
            resetViewState();
            filteredRows = opened.allRows();
            session.set(opened);
            rows.set(filteredRows);
            if (previous != null) {
                previous.close();
            }
            remember(file);
        });
    }

    public void applyFilter() {
        ViewerSession current = session.get();
        if (current == null) {
            return;
        }
        ViewFilter filter = new ViewFilter(quickSearch.get(), query.get());
        SortSpec sort = sortSpec.get();
        queryError.set(null);
        track(
                (progress, cancellation) -> {
                    int[] filtered = useCases.filter().execute(current, filter, progress, cancellation);
                    int[] sorted = sort == null
                            ? filtered
                            : useCases.sort().execute(current, filtered, sort, progress, cancellation);
                    return new View(filtered, sorted);
                },
                view -> {
                    filteredRows = view.filtered();
                    rows.set(view.sorted());
                });
    }

    public void resetFilter() {
        quickSearch.set("");
        query.set("");
        applyFilter();
    }

    /** @param spec {@code null} = urutan file */
    public void requestSort(SortSpec spec) {
        ViewerSession current = session.get();
        if (current == null || Objects.equals(spec, sortSpec.get())) {
            return;
        }
        sortSpec.set(spec);
        if (spec == null) {
            rows.set(filteredRows);
            return;
        }
        int[] base = filteredRows;
        track(
                (progress, cancellation) -> useCases.sort().execute(current, base, spec, progress, cancellation),
                rows::set);
    }

    public void select(int record) {
        selectedRecord.set(record);
    }

    /** Posisi di view untuk nomor record (mulai 1) yang diketik user. */
    public OptionalInt positionOf(String text) {
        int number;
        try {
            number = Integer.parseInt(text.strip().replace(".", ""));
        } catch (NumberFormatException e) {
            context.notifier().warning(Messages.format("json.viewer.jump.invalid", text));
            return OptionalInt.empty();
        }
        int[] view = rows.get();
        for (int position = 0; view != null && position < view.length; position++) {
            if (view[position] == number - 1) {
                return OptionalInt.of(position);
            }
        }
        context.notifier().warning(Messages.format("json.viewer.jump.notFound", number));
        return OptionalInt.empty();
    }

    public String columnTitle(RecordColumn column) {
        return switch (column) {
            case RecordColumn.RecordNumber number -> number.header();
            case RecordColumn.WholeValue ignored -> Messages.get("json.viewer.column.value");
            case RecordColumn.Field field -> field.key();
        };
    }

    /** Dipanggil saat render sel di FX thread; record dibaca dari cache LRU. */
    public String cellText(int record, RecordColumn column) {
        if (column instanceof RecordColumn.RecordNumber) {
            return String.valueOf(record + 1);
        }
        ViewerSession current = session.get();
        if (current == null) {
            return "";
        }
        try {
            return column.extract(current.record(record))
                    .map(value -> value.displayText(CELL_PREVIEW_MAX_CHARS))
                    .orElse("");
        } catch (JsonToolException e) {
            return Messages.get("json.viewer.cell.invalid");
        }
    }

    public String rowJson(int record) {
        return new String(session.get().raw(record), StandardCharsets.UTF_8);
    }

    public void notifyCopied() {
        context.notifier().info(Messages.get("json.viewer.copied"));
    }

    /** Teks panel detail: pretty untuk record ≤ 1 MB, selebihnya potongan teks mentah. */
    public Detail detail(int record) {
        ViewerSession current = session.get();
        try {
            if (current.index().length(record) > DETAIL_MAX_BYTES) {
                return new Detail(current.rawPrefix(record, DETAIL_MAX_BYTES), true);
            }
            return new Detail(JsonWriter.pretty(current.record(record)), false);
        } catch (JsonToolException e) {
            return new Detail(JsonErrorMessages.header(e) + "\n" + JsonErrorMessages.detail(e), false);
        }
    }

    public void loadRecordNode(int record, Consumer<JsonNodeInfo> onLoaded) {
        ViewerSession current = session.get();
        background((progress, cancellation) -> current.recordNode(record), onLoaded);
    }

    /** Root mode tree; membaca seluruh file sehingga ditampilkan dengan progress. */
    public void loadRootNode(Consumer<JsonNodeInfo> onLoaded) {
        ViewerSession current = session.get();
        track((progress, cancellation) -> current.recordNode(0), onLoaded);
    }

    public void loadChildren(JsonNodeInfo node, Consumer<NodeChildren> onLoaded) {
        ViewerSession current = session.get();
        background((progress, cancellation) -> current.children(node), onLoaded);
    }

    public void download() {
        export((file, name) -> context.downloads().download(file, name));
    }

    public void saveAs() {
        export((file, name) -> context.downloads().saveAs(file, name));
    }

    public String exportLabel(ExportFormat format) {
        return switch (format) {
            case JSON_ARRAY -> Messages.get("json.viewer.export.json");
            case JSON_LINES -> Messages.get("json.viewer.export.jsonl");
            case CSV -> Messages.get("json.viewer.export.csv");
        };
    }

    public StringBinding fileSummary() {
        return Bindings.createStringBinding(
                () -> {
                    ViewerSession current = session.get();
                    if (current == null) {
                        return Messages.get("json.viewer.noFile");
                    }
                    return Messages.format(
                            "json.viewer.file.summary",
                            current.index().file().getFileName(),
                            ByteSizeFormat.format(current.index().fileSize()),
                            formatLabel(current));
                },
                session);
    }

    /** Status bar: ukuran file, total/terfilter, waktu indexing, dan memori (diperbarui tiap 2 detik). */
    public StringBinding statusText() {
        return Bindings.createStringBinding(
                () -> {
                    ViewerSession current = session.get();
                    if (current == null) {
                        return memory.get();
                    }
                    int shown = rows.get() == null ? 0 : rows.get().length;
                    double seconds = current.index().indexingTime().toNanos() / NANOS_PER_SECOND;
                    return Messages.format(
                            "json.viewer.status",
                            ByteSizeFormat.format(current.index().fileSize()),
                            current.recordCount(),
                            shown,
                            seconds,
                            memory.get());
                },
                session,
                rows,
                memory);
    }

    public ReadOnlyStringProperty memoryProperty() {
        return memory;
    }

    public void dispose() {
        disposed = true;
        memoryTimer.stop();
        cancelAll();
        if (session.get() != null) {
            session.get().close();
            session.set(null);
        }
        rows.set(null);
        filteredRows = new int[0];
        tempFiles.close();
    }

    private void export(BiConsumer<Path, String> deliver) {
        ViewerSession current = session.get();
        int[] view = rows.get();
        ExportFormat format = exportFormat.get();
        if (current == null || view == null) {
            return;
        }
        // Satu temp file per format dipakai ulang supaya export berulang tidak menumpuk di disk
        Path output = exportFiles.computeIfAbsent(format, key -> tempFiles.createFile("." + key.fileName("x")));
        String name = format.fileName(FileExtensions.baseName(current.index().file()));
        track(
                (progress, cancellation) ->
                        useCases.export().execute(current, view, format, output, progress, cancellation),
                exported -> deliver.accept(exported, name));
    }

    private void remember(Path file) {
        background((progress, cancellation) -> useCases.recent().remember(file), recentFiles::setAll);
    }

    private String formatLabel(ViewerSession current) {
        return switch (current.format()) {
            case ARRAY -> Messages.get("json.viewer.format.array");
            case JSON_LINES -> Messages.get("json.viewer.format.lines");
            case SINGLE_VALUE -> Messages.get("json.viewer.format.tree");
        };
    }

    private void resetViewState() {
        sortSpec.set(null);
        quickSearch.set("");
        query.set("");
        queryError.set(null);
        selectedRecord.set(-1);
    }

    private void refreshMemory() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        memory.set(Messages.format(
                "json.viewer.memory", ByteSizeFormat.format(used), ByteSizeFormat.format(runtime.maxMemory())));
    }

    /** Proses utama: tampil di status bar dengan tombol Batal, kontrol lain dinonaktifkan. */
    private <T> void track(BackgroundWork<T> work, Consumer<T> onSuccess) {
        RunningTask task = context.tasks().submit(work, onSuccess::accept, this::showFailure);
        runningTask = task;
        busy.set(true);
        task.runningProperty().addListener((observable, wasRunning, running) -> busy.set(running));
        currentTask.set(task);
    }

    /** Proses ringan (tree, recent) yang tidak mengunci UI, tetap dibatalkan saat file/tool berganti. */
    private <T> void background(BackgroundWork<T> work, Consumer<T> onSuccess) {
        RunningTask task = context.tasks().submit(work, onSuccess::accept, this::showFailure);
        backgroundTasks.add(task);
        task.runningProperty().addListener((observable, wasRunning, running) -> {
            if (!running) {
                backgroundTasks.remove(task);
            }
        });
    }

    private void showFailure(Throwable failure) {
        if (disposed) {
            return;
        }
        if (failure instanceof InvalidQueryException invalid) {
            queryError.set(invalid);
            return;
        }
        JsonErrorMessages.show(context.owner(), failure);
    }

    private void cancelAll() {
        if (runningTask != null) {
            runningTask.cancel();
        }
        List.copyOf(backgroundTasks).forEach(RunningTask::cancel);
        backgroundTasks.clear();
    }

    public record Detail(String text, boolean truncated) {}

    private record View(int[] filtered, int[] sorted) {}
}
