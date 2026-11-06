package io.github.lukmnh.toolbox.devutils.sql.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ClipboardActions;
import io.github.lukmnh.toolbox.devutils.sql.domain.DataTable;
import io.github.lukmnh.toolbox.devutils.sql.domain.DmlOperation;
import io.github.lukmnh.toolbox.devutils.sql.domain.GenerateOptions;
import io.github.lukmnh.toolbox.devutils.sql.domain.GeneratedSyntax;
import io.github.lukmnh.toolbox.devutils.sql.domain.SyntaxTarget;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** State dan aksi SQL Generator: muat data → pilih database/operasi/tabel/key → generate → copy/download. */
final class GeneratorViewModel {

    /** Hasil generate beserta opsi yang dipakai (untuk info dan nama file download). */
    record Run(SyntaxTarget target, DmlOperation operation, GeneratedSyntax syntax, long outputBytes) {}

    private static final String DEFAULT_KEY = "id";
    private static final Pattern NOT_IDENTIFIER = Pattern.compile("[^A-Za-z0-9_]+");

    private final SqlUseCases useCases;
    private final ToolContext context;
    private final TempFiles tempFiles;

    private final ObjectProperty<DataTable> data = new SimpleObjectProperty<>();
    private final ObjectProperty<SyntaxTarget> target = new SimpleObjectProperty<>(SyntaxTarget.POSTGRESQL);
    private final ObjectProperty<DmlOperation> operation = new SimpleObjectProperty<>(DmlOperation.INSERT);
    private final StringProperty table = new SimpleStringProperty("");
    private final ObjectProperty<String> keyColumn = new SimpleObjectProperty<>();
    private final BooleanProperty allText = new SimpleBooleanProperty();

    private final ObjectProperty<Run> result = new SimpleObjectProperty<>();
    private final StringProperty preview = new SimpleStringProperty("");
    private final BooleanProperty previewTruncated = new SimpleBooleanProperty();
    private final BooleanProperty stale = new SimpleBooleanProperty();

    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final ObjectProperty<RunningTask> currentTask = new SimpleObjectProperty<>();
    private RunningTask runningTask;
    /** Satu temp file per ekstensi dipakai ulang supaya download berulang tidak menumpuk di disk. */
    private final Map<String, Path> outputFiles = new HashMap<>();

    GeneratorViewModel(SqlUseCases useCases, ToolContext context, TempFiles tempFiles) {
        this.useCases = useCases;
        this.context = context;
        this.tempFiles = tempFiles;
        markStaleOnChange(data, target, operation, table, keyColumn, allText);
    }

    ReadOnlyObjectProperty<DataTable> dataProperty() {
        return data;
    }

    ObjectProperty<SyntaxTarget> targetProperty() {
        return target;
    }

    ObjectProperty<DmlOperation> operationProperty() {
        return operation;
    }

    StringProperty tableProperty() {
        return table;
    }

    ObjectProperty<String> keyColumnProperty() {
        return keyColumn;
    }

    BooleanProperty allTextProperty() {
        return allText;
    }

    ReadOnlyObjectProperty<Run> resultProperty() {
        return result;
    }

    ReadOnlyStringProperty previewProperty() {
        return preview;
    }

    ReadOnlyBooleanProperty previewTruncatedProperty() {
        return previewTruncated;
    }

    ReadOnlyBooleanProperty staleProperty() {
        return stale;
    }

    ReadOnlyObjectProperty<RunningTask> currentTaskProperty() {
        return currentTask;
    }

    /** Alasan Generate belum bisa dijalankan, atau string kosong bila siap. */
    StringBinding problem() {
        return Bindings.createStringBinding(
                () -> {
                    DataTable loaded = data.get();
                    if (loaded == null) {
                        return "";
                    }
                    if (table.get().isBlank()) {
                        return Messages.get("devutils.sql.generator.problem.table");
                    }
                    if (operation.get().needsKey() && keyColumn.get() == null) {
                        return Messages.get("devutils.sql.generator.problem.key");
                    }
                    if (operation.get() == DmlOperation.UPDATE
                            && loaded.columns().size() < 2) {
                        return Messages.get("devutils.sql.generator.problem.updateColumns");
                    }
                    return "";
                },
                data,
                table,
                operation,
                keyColumn);
    }

    BooleanBinding canGenerate() {
        return busy.not().and(data.isNotNull()).and(problem().isEmpty());
    }

    BooleanBinding canExport() {
        return result.isNotNull().and(busy.not());
    }

    void loadFile(Path file) {
        track(context.tasks()
                .submit(
                        (progress, cancellation) -> useCases.loadData().execute(file),
                        this::showLoaded,
                        error -> SqlErrorMessages.show(context.owner(), error)));
    }

    void clear() {
        data.set(null);
        keyColumn.set(null);
        table.set("");
        result.set(null);
        preview.set("");
        previewTruncated.set(false);
        stale.set(false);
    }

    /** Opsi dibaca di FX thread lalu diteruskan ke background, supaya worker tidak menyentuh property JavaFX. */
    void generate() {
        DataTable loaded = data.get();
        if (loaded == null) {
            return;
        }
        GenerateOptions options = new GenerateOptions(
                target.get(),
                operation.get(),
                table.get(),
                operation.get().needsKey() ? keyColumn.get() : null,
                allText.get());
        track(context.tasks()
                .submit(
                        (progress, cancellation) -> {
                            GeneratedSyntax syntax = useCases.generate().execute(loaded, options, cancellation);
                            return new Run(
                                    options.target(), options.operation(), syntax, SqlRun.utf8Length(syntax.text()));
                        },
                        this::showResult,
                        error -> SqlErrorMessages.show(context.owner(), error)));
    }

    void copy() {
        if (result.get() != null) {
            ClipboardActions.copy(result.get().syntax().text(), context.notifier());
        }
    }

    void download() {
        export((file, name) -> context.downloads().download(file, name));
    }

    void saveAs() {
        export((file, name) -> context.downloads().saveAs(file, name));
    }

    void dispose() {
        if (runningTask != null) {
            runningTask.cancel();
        }
        tempFiles.close();
    }

    /** Nama tabel default dari nama file; key default kolom {@code id} bila ada, selain itu kolom pertama. */
    private void showLoaded(DataTable loaded) {
        List<String> columns = loaded.columns();
        data.set(loaded);
        table.set(defaultTableName(loaded.sourceName()));
        keyColumn.set(columns.stream()
                .filter(column -> column.equalsIgnoreCase(DEFAULT_KEY))
                .findFirst()
                .orElse(columns.getFirst()));
    }

    static String defaultTableName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String base = dot <= 0 ? fileName : fileName.substring(0, dot);
        String cleaned = NOT_IDENTIFIER.matcher(base.strip()).replaceAll("_").toLowerCase(Locale.ROOT);
        return cleaned.isEmpty() || Character.isDigit(cleaned.charAt(0)) ? "t_" + cleaned : cleaned;
    }

    private void showResult(Run run) {
        String text = run.syntax().text();
        boolean truncated = text.length() > SqlViewModel.EDITOR_MAX_CHARS;
        preview.set(truncated ? SqlViewModel.safePrefix(text, SqlViewModel.EDITOR_MAX_CHARS) : text);
        previewTruncated.set(truncated);
        result.set(run);
        stale.set(false);
    }

    private void export(BiConsumer<Path, String> deliver) {
        Run current = result.get();
        DataTable loaded = data.get();
        if (current == null || loaded == null) {
            return;
        }
        String extension = current.target().extension();
        String name = defaultTableName(loaded.sourceName()) + "."
                + current.operation().name().toLowerCase(Locale.ROOT) + "." + extension;
        Path target = outputFiles.computeIfAbsent(extension, ext -> tempFiles.createFile("." + ext));
        String text = current.syntax().text();
        track(context.tasks()
                .submit(
                        (progress, cancellation) -> {
                            useCases.export().execute(text, target);
                            return target;
                        },
                        file -> deliver.accept(file, name),
                        error -> SqlErrorMessages.show(context.owner(), error)));
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
