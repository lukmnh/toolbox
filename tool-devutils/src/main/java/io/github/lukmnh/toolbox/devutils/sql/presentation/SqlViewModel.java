package io.github.lukmnh.toolbox.devutils.sql.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.devutils.shared.presentation.ClipboardActions;
import io.github.lukmnh.toolbox.devutils.sql.domain.FormatOptions;
import io.github.lukmnh.toolbox.devutils.sql.domain.IndentStyle;
import io.github.lukmnh.toolbox.devutils.sql.domain.LoadedSql;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlDialect;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlOutput;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
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

/** State dan aksi SQL Formatter. */
final class SqlViewModel {

    /** Teks lebih panjang dari ini tidak dimuat ke {@code TextArea} karena membuat UI tersendat. */
    static final int EDITOR_MAX_CHARS = 1_000_000;

    static final int DEFAULT_BLANK_LINES = 1;
    private static final String PASTED_BASE_NAME = "pasted";
    private static final String SQL_EXTENSION = ".sql";

    private final SqlUseCases useCases;
    private final ToolContext context;
    private final TempFiles tempFiles;

    private final StringProperty input = new SimpleStringProperty("");
    private final ObjectProperty<LoadedSql> largeFile = new SimpleObjectProperty<>();
    private final StringProperty fileName = new SimpleStringProperty("");
    private final ObjectProperty<SqlDialect> dialect = new SimpleObjectProperty<>(SqlDialect.STANDARD);
    private final ObjectProperty<IndentStyle> indent = new SimpleObjectProperty<>(IndentStyle.TWO_SPACES);
    private final BooleanProperty uppercaseKeywords = new SimpleBooleanProperty(true);
    private final ObjectProperty<Integer> blankLines = new SimpleObjectProperty<>(DEFAULT_BLANK_LINES);
    private final BooleanProperty removeComments = new SimpleBooleanProperty(true);

    private final ObjectProperty<SqlRun> result = new SimpleObjectProperty<>();
    private final StringProperty preview = new SimpleStringProperty("");
    private final BooleanProperty previewTruncated = new SimpleBooleanProperty();
    private final BooleanProperty stale = new SimpleBooleanProperty();

    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final ObjectProperty<RunningTask> currentTask = new SimpleObjectProperty<>();
    private RunningTask runningTask;
    /** Satu temp file dipakai ulang supaya download berulang tidak menumpuk di disk. */
    private Path outputFile;

    SqlViewModel(SqlUseCases useCases, ToolContext context, TempFiles tempFiles) {
        this.useCases = useCases;
        this.context = context;
        this.tempFiles = tempFiles;
        markStaleOnChange(input, largeFile, dialect, indent, uppercaseKeywords, blankLines, removeComments);
    }

    StringProperty inputProperty() {
        return input;
    }

    ReadOnlyObjectProperty<LoadedSql> largeFileProperty() {
        return largeFile;
    }

    ReadOnlyStringProperty fileNameProperty() {
        return fileName;
    }

    ObjectProperty<SqlDialect> dialectProperty() {
        return dialect;
    }

    ObjectProperty<IndentStyle> indentProperty() {
        return indent;
    }

    BooleanProperty uppercaseKeywordsProperty() {
        return uppercaseKeywords;
    }

    ObjectProperty<Integer> blankLinesProperty() {
        return blankLines;
    }

    BooleanProperty removeCommentsProperty() {
        return removeComments;
    }

    ReadOnlyObjectProperty<SqlRun> resultProperty() {
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

    BooleanBinding isEmpty() {
        return Bindings.createBooleanBinding(
                () -> largeFile.get() == null && input.get().isBlank(), input, largeFile);
    }

    BooleanBinding canRun() {
        return busy.not().and(isEmpty().not());
    }

    BooleanBinding canExport() {
        return result.isNotNull().and(busy.not());
    }

    void format() {
        FormatOptions options =
                new FormatOptions(dialect.get(), indent.get(), uppercaseKeywords.get(), blankLines.get());
        run(SqlAction.FORMAT, (sql, cancellation) -> useCases.format().execute(sql, options, cancellation));
    }

    void minify() {
        SqlDialect selected = dialect.get();
        boolean dropComments = removeComments.get();
        run(
                SqlAction.MINIFY,
                (sql, cancellation) -> useCases.minify().execute(sql, selected, dropComments, cancellation));
    }

    void loadFile(Path file) {
        track(context.tasks()
                .submit(
                        (progress, cancellation) -> useCases.load().execute(file),
                        this::showLoaded,
                        error -> SqlErrorMessages.show(context.owner(), error)));
    }

    void clear() {
        largeFile.set(null);
        input.set("");
        fileName.set("");
    }

    void copy() {
        if (result.get() != null) {
            ClipboardActions.copy(result.get().text(), context.notifier());
        }
    }

    void download() {
        export((file, name) -> context.downloads().download(file, name));
    }

    void saveAs() {
        export((file, name) -> context.downloads().saveAs(file, name));
    }

    String inputSummary() {
        LoadedSql large = largeFile.get();
        if (large != null) {
            return Messages.format(
                    "devutils.sql.input.largeFile", large.fileName(), ByteSizeFormat.format(large.sizeBytes()));
        }
        return fileName.get().isEmpty()
                ? Messages.get("devutils.sql.input.pasted")
                : Messages.format("devutils.sql.input.file", fileName.get());
    }

    void dispose() {
        if (runningTask != null) {
            runningTask.cancel();
        }
        tempFiles.close();
    }

    /** Teks dibaca di FX thread lalu diteruskan ke background, supaya worker tidak menyentuh property JavaFX. */
    private void run(SqlAction action, BiFunction<String, CancellationToken, SqlOutput> work) {
        String sql = effectiveText();
        SqlDialect selected = dialect.get();
        track(context.tasks()
                .submit(
                        (progress, cancellation) -> SqlRun.of(action, selected, sql, work.apply(sql, cancellation)),
                        this::showResult,
                        error -> SqlErrorMessages.show(context.owner(), error)));
    }

    private String effectiveText() {
        return largeFile.get() != null ? largeFile.get().text() : input.get();
    }

    private void showLoaded(LoadedSql loaded) {
        if (loaded.text().length() <= EDITOR_MAX_CHARS) {
            largeFile.set(null);
            input.set(loaded.text());
        } else {
            input.set("");
            largeFile.set(loaded);
        }
        fileName.set(loaded.fileName());
    }

    private void showResult(SqlRun run) {
        String text = run.text();
        boolean truncated = text.length() > EDITOR_MAX_CHARS;
        preview.set(truncated ? safePrefix(text, EDITOR_MAX_CHARS) : text);
        previewTruncated.set(truncated);
        result.set(run);
        stale.set(false);
    }

    /** Potong tanpa memisahkan pasangan surrogate (emoji di komentar/string). */
    private static String safePrefix(String text, int length) {
        int end = Character.isHighSurrogate(text.charAt(length - 1)) ? length - 1 : length;
        return text.substring(0, end);
    }

    private void export(BiConsumer<Path, String> deliver) {
        SqlRun current = result.get();
        if (current == null) {
            return;
        }
        String name = baseName() + current.action().fileSuffix();
        Path target = outputFile();
        track(context.tasks()
                .submit(
                        (progress, cancellation) -> {
                            useCases.export().execute(current.text(), target);
                            return target;
                        },
                        file -> deliver.accept(file, name),
                        error -> SqlErrorMessages.show(context.owner(), error)));
    }

    private String baseName() {
        String name = fileName.get();
        if (name.isEmpty()) {
            return PASTED_BASE_NAME;
        }
        int dot = name.lastIndexOf('.');
        return dot <= 0 ? name : name.substring(0, dot);
    }

    private Path outputFile() {
        if (outputFile == null) {
            outputFile = tempFiles.createFile(SQL_EXTENSION);
        }
        return outputFile;
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
