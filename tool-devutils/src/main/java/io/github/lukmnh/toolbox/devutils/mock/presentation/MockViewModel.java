package io.github.lukmnh.toolbox.devutils.mock.presentation;

import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ClipboardActions;
import io.github.lukmnh.toolbox.common.ui.Debounce;
import io.github.lukmnh.toolbox.devutils.mock.application.PreviewMockUseCase;
import io.github.lukmnh.toolbox.devutils.mock.domain.InvalidMockSchemaException;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockFormat;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockLocale;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockSchema;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockSchemaValidator;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockToolException;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockType;
import java.nio.file.Path;
import java.util.SplittableRandom;
import java.util.function.BiConsumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.util.Duration;

/**
 * State Mock Data Generator. Setiap perubahan kolom/setting memperbarui preview (debounce 250 ms, sinkron karena hanya
 * {@code PREVIEW_ROWS} baris). Download menulis semua baris di background lewat satu temp file yang dipakai ulang.
 */
final class MockViewModel {

    static final String DEFAULT_TABLE = "users";
    static final int DEFAULT_ROWS = 100;
    private static final String FILE_BASE_NAME = "mock-data";
    private static final String TEMP_SUFFIX = ".mock";
    private static final long MAX_GENERATED_SEED = 1_000_000;
    private static final Duration DEBOUNCE = Duration.millis(250);

    private final MockUseCases useCases;
    private final ToolContext context;
    private final TempFiles tempFiles;
    private final Debounce debounce = new Debounce(DEBOUNCE, this::generatePreview);
    // Hanya untuk mengusulkan seed baru; tidak dipakai untuk keamanan
    private final SplittableRandom seeds = new SplittableRandom();

    private final ObservableList<ColumnDraft> columns = FXCollections.observableArrayList();
    private final ObjectProperty<MockLocale> locale = new SimpleObjectProperty<>(MockLocale.ID);
    private final ObjectProperty<Integer> rows = new SimpleObjectProperty<>(DEFAULT_ROWS);
    private final StringProperty seed = new SimpleStringProperty();
    private final ObjectProperty<MockFormat> format = new SimpleObjectProperty<>(MockFormat.JSON);
    private final StringProperty table = new SimpleStringProperty(DEFAULT_TABLE);

    private final StringProperty preview = new SimpleStringProperty("");
    private final StringProperty error = new SimpleStringProperty("");
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final ObjectProperty<RunningTask> currentTask = new SimpleObjectProperty<>();
    private RunningTask runningTask;
    /** Satu temp file dipakai ulang supaya download berulang tidak menumpuk di disk. */
    private Path outputFile;

    MockViewModel(MockUseCases useCases, ToolContext context, TempFiles tempFiles) {
        this.useCases = useCases;
        this.context = context;
        this.tempFiles = tempFiles;
        newSeed();
        addColumn("id", MockType.SEQUENCE);
        addColumn("name", MockType.NAME);
        addColumn("email", MockType.EMAIL);
        addColumn("city", MockType.CITY);
        addColumn("created_at", MockType.DATE);
        addColumn("active", MockType.BOOLEAN);
        // ChangeListener, bukan InvalidationListener: tetap terpicu walau preview gagal sebelum semua nilai dibaca
        refreshOnChange(locale);
        refreshOnChange(rows);
        refreshOnChange(seed);
        refreshOnChange(format);
        refreshOnChange(table);
        columns.addListener((ListChangeListener<ColumnDraft>) change -> debounce.trigger());
        generatePreview();
    }

    private void refreshOnChange(ObservableValue<?> source) {
        source.addListener((observable, previous, current) -> debounce.trigger());
    }

    void addColumn() {
        if (columns.size() >= MockSchemaValidator.MAX_COLUMNS) {
            return;
        }
        int number = columns.size() + 1;
        while (hasColumn("column_" + number)) {
            number++;
        }
        addColumn("column_" + number, MockType.NAME);
    }

    private void addColumn(String name, MockType type) {
        ColumnDraft draft = new ColumnDraft(name, type);
        draft.onChange(debounce::trigger);
        columns.add(draft);
    }

    private boolean hasColumn(String name) {
        return columns.stream()
                .anyMatch(draft -> draft.nameProperty().get().strip().equalsIgnoreCase(name));
    }

    /** Minimal satu kolom tersisa. */
    void removeColumn(ColumnDraft draft) {
        if (columns.size() > 1) {
            columns.remove(draft);
        }
    }

    void newSeed() {
        seed.set(String.valueOf(seeds.nextLong(1, MAX_GENERATED_SEED)));
    }

    void generatePreview() {
        try {
            preview.set(useCases.preview().execute(schema(), format.get()));
            error.set("");
        } catch (MockToolException e) {
            preview.set("");
            error.set(MockTexts.describe(e));
        }
    }

    void copy() {
        ClipboardActions.copy(preview.get(), context.notifier());
    }

    void download() {
        export((file, name) -> context.downloads().download(file, name));
    }

    void saveAs() {
        export((file, name) -> context.downloads().saveAs(file, name));
    }

    void dispose() {
        debounce.stop();
        if (runningTask != null) {
            runningTask.cancel();
        }
        tempFiles.close();
    }

    /** Schema dibaca di FX thread lalu diteruskan ke background, supaya worker tidak menyentuh property JavaFX. */
    private void export(BiConsumer<Path, String> deliver) {
        MockSchema current;
        try {
            current = schema();
            MockSchemaValidator.validate(current);
        } catch (MockToolException e) {
            error.set(MockTexts.describe(e));
            return;
        }
        MockFormat selected = format.get();
        Path target = outputFile();
        String name = FILE_BASE_NAME + "." + selected.extension();
        track(context.tasks()
                .submit(
                        (progress, cancellation) ->
                                useCases.export().execute(current, selected, target, progress, cancellation),
                        file -> deliver.accept(file, name),
                        failure -> MockTexts.show(context.owner(), failure)));
    }

    private MockSchema schema() {
        long seedValue;
        try {
            seedValue = Long.parseLong(seed.get().strip());
        } catch (NumberFormatException e) {
            throw new InvalidMockSchemaException(InvalidMockSchemaException.Reason.INVALID_SEED, seed.get());
        }
        return new MockSchema(
                columns.stream().map(ColumnDraft::toColumn).toList(),
                locale.get(),
                seedValue,
                rows.get(),
                table.get().strip());
    }

    private Path outputFile() {
        if (outputFile == null) {
            outputFile = tempFiles.createFile(TEMP_SUFFIX);
        }
        return outputFile;
    }

    private void track(RunningTask task) {
        runningTask = task;
        busy.set(true);
        task.runningProperty().addListener((observable, wasRunning, running) -> busy.set(running));
        currentTask.set(task);
    }

    /** Jumlah baris yang tampil di preview (untuk teks info "x of y rows"). */
    int previewRows() {
        return Math.min(rows.get(), PreviewMockUseCase.PREVIEW_ROWS);
    }

    ObservableList<ColumnDraft> columns() {
        return columns;
    }

    ObjectProperty<MockLocale> localeProperty() {
        return locale;
    }

    ObjectProperty<Integer> rowsProperty() {
        return rows;
    }

    StringProperty seedProperty() {
        return seed;
    }

    ObjectProperty<MockFormat> formatProperty() {
        return format;
    }

    StringProperty tableProperty() {
        return table;
    }

    ReadOnlyStringProperty previewProperty() {
        return preview;
    }

    ReadOnlyStringProperty errorProperty() {
        return error;
    }

    ReadOnlyBooleanProperty busyProperty() {
        return busy;
    }

    ReadOnlyObjectProperty<RunningTask> currentTaskProperty() {
        return currentTask;
    }
}
