package io.github.lukmnh.toolbox.json.csv.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.common.ui.Debounce;
import io.github.lukmnh.toolbox.json.csv.domain.ArrangeStep;
import io.github.lukmnh.toolbox.json.csv.domain.ColumnPipeline;
import io.github.lukmnh.toolbox.json.csv.domain.CsvEncoding;
import io.github.lukmnh.toolbox.json.csv.domain.CsvSample;
import io.github.lukmnh.toolbox.json.csv.domain.CsvSource;
import io.github.lukmnh.toolbox.json.csv.domain.Delimiter;
import io.github.lukmnh.toolbox.json.csv.domain.ExportRequest;
import io.github.lukmnh.toolbox.json.csv.domain.ExportResult;
import io.github.lukmnh.toolbox.json.csv.domain.InputFormat;
import io.github.lukmnh.toolbox.json.csv.domain.InvalidStepException;
import io.github.lukmnh.toolbox.json.csv.domain.LineEnding;
import io.github.lukmnh.toolbox.json.csv.domain.MergeStep;
import io.github.lukmnh.toolbox.json.csv.domain.MismatchPolicy;
import io.github.lukmnh.toolbox.json.csv.domain.OutputDelimiter;
import io.github.lukmnh.toolbox.json.csv.domain.OutputOptions;
import io.github.lukmnh.toolbox.json.csv.domain.PipelinePreview;
import io.github.lukmnh.toolbox.json.csv.domain.PipelineStep;
import io.github.lukmnh.toolbox.json.csv.domain.QuoteMode;
import io.github.lukmnh.toolbox.json.csv.domain.RegexSplitException;
import io.github.lukmnh.toolbox.json.csv.domain.SplitStep;
import io.github.lukmnh.toolbox.json.csv.domain.TablePreview;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
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
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.util.Duration;

/** State dan aksi CSV Split & Merge: file → format → pipeline langkah → pratinjau → ekspor streaming. */
public final class CsvViewModel {

    /** Jeda setelah aturan berhenti berubah sebelum pratinjau dihitung ulang. */
    private static final Duration PREVIEW_DELAY = Duration.millis(250);

    private static final double NANOS_PER_SECOND = 1e9;
    private static final int NO_SELECTION = -1;

    private final CsvUseCases useCases;
    private final ToolContext context;
    private final TempFiles tempFiles;
    private final Debounce previewDebounce;

    private final ObjectProperty<CsvSource> source = new SimpleObjectProperty<>();
    private final ObjectProperty<Delimiter> delimiter = new SimpleObjectProperty<>(Delimiter.COMMA);
    private final ObjectProperty<CsvEncoding> encoding = new SimpleObjectProperty<>(CsvEncoding.UTF_8);
    private final BooleanProperty hasHeader = new SimpleBooleanProperty(true);
    private final ObjectProperty<MismatchPolicy> mismatchPolicy = new SimpleObjectProperty<>(MismatchPolicy.PAD);

    private final ObjectProperty<CsvSample> sample = new SimpleObjectProperty<>();
    private final ObjectProperty<TablePreview> inputTable = new SimpleObjectProperty<>();
    private final ObservableList<PipelineStep> steps = FXCollections.observableArrayList();
    private final ObservableList<PipelineStep> readOnlySteps = FXCollections.unmodifiableObservableList(steps);
    private final IntegerProperty selectedIndex = new SimpleIntegerProperty(NO_SELECTION);
    /** Naik setiap kali editor langkah terpilih harus dibangun ulang (pilihan/kolom tersedia berubah). */
    private final IntegerProperty editorRevision = new SimpleIntegerProperty();

    private final ObjectProperty<PipelinePreview> preview = new SimpleObjectProperty<>();
    private final StringProperty previewError = new SimpleStringProperty("");

    private final ObjectProperty<OutputDelimiter> outputDelimiter =
            new SimpleObjectProperty<>(OutputDelimiter.SAME_AS_INPUT);
    private final ObjectProperty<CsvEncoding> outputEncoding = new SimpleObjectProperty<>(CsvEncoding.UTF_8);
    private final ObjectProperty<QuoteMode> quote = new SimpleObjectProperty<>(QuoteMode.MINIMAL);
    private final ObjectProperty<LineEnding> lineEnding = new SimpleObjectProperty<>(LineEnding.CRLF);
    private final BooleanProperty includeHeader = new SimpleBooleanProperty(true);
    private final BooleanProperty neutralizeFormulas = new SimpleBooleanProperty(true);
    private final ObjectProperty<ExportResult> result = new SimpleObjectProperty<>();

    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final ObjectProperty<RunningTask> currentTask = new SimpleObjectProperty<>();
    private RunningTask loadTask;
    private RunningTask previewTask;
    private RunningTask exportTask;
    /** Hasil pratinjau lama yang selesai belakangan diabaikan lewat nomor generasi ini. */
    private long previewGeneration;

    private boolean applyingDetectedFormat;
    private boolean editingSelectedStep;
    /** Satu temp file dipakai ulang supaya ekspor berulang dari file ratusan MB tidak menumpuk di disk. */
    private Path outputFile;

    public CsvViewModel(CsvUseCases useCases, ToolContext context, TempFiles tempFiles) {
        this.useCases = useCases;
        this.context = context;
        this.tempFiles = tempFiles;
        this.previewDebounce = new Debounce(PREVIEW_DELAY, this::refreshPreview);
        listenToInputFormat();
        listenToOutputOptions();
        steps.addListener((ListChangeListener<PipelineStep>) change -> {
            if (!editingSelectedStep) {
                bumpEditor();
            }
            schedulePreview();
        });
    }

    // ----- Properti untuk view -----

    public ReadOnlyObjectProperty<CsvSource> sourceProperty() {
        return source;
    }

    public ObjectProperty<Delimiter> delimiterProperty() {
        return delimiter;
    }

    public ObjectProperty<CsvEncoding> encodingProperty() {
        return encoding;
    }

    public BooleanProperty hasHeaderProperty() {
        return hasHeader;
    }

    public ObjectProperty<MismatchPolicy> mismatchPolicyProperty() {
        return mismatchPolicy;
    }

    public ReadOnlyObjectProperty<CsvSample> sampleProperty() {
        return sample;
    }

    public ReadOnlyObjectProperty<TablePreview> inputTableProperty() {
        return inputTable;
    }

    public ObservableList<PipelineStep> steps() {
        return readOnlySteps;
    }

    public ReadOnlyIntegerProperty selectedIndexProperty() {
        return selectedIndex;
    }

    public ReadOnlyIntegerProperty editorRevisionProperty() {
        return editorRevision;
    }

    public ReadOnlyObjectProperty<PipelinePreview> previewProperty() {
        return preview;
    }

    public ReadOnlyStringProperty previewErrorProperty() {
        return previewError;
    }

    public ObjectProperty<OutputDelimiter> outputDelimiterProperty() {
        return outputDelimiter;
    }

    public ObjectProperty<CsvEncoding> outputEncodingProperty() {
        return outputEncoding;
    }

    public ObjectProperty<QuoteMode> quoteProperty() {
        return quote;
    }

    public ObjectProperty<LineEnding> lineEndingProperty() {
        return lineEnding;
    }

    public BooleanProperty includeHeaderProperty() {
        return includeHeader;
    }

    public BooleanProperty neutralizeFormulasProperty() {
        return neutralizeFormulas;
    }

    public ReadOnlyObjectProperty<ExportResult> resultProperty() {
        return result;
    }

    public ReadOnlyObjectProperty<RunningTask> currentTaskProperty() {
        return currentTask;
    }

    public BooleanBinding canExport() {
        return busy.not().and(preview.isNotNull()).and(previewError.isEmpty());
    }

    public BooleanBinding hasSelection() {
        return selectedIndex.greaterThanOrEqualTo(0);
    }

    public BooleanBinding canMoveUp() {
        return selectedIndex.greaterThan(0);
    }

    public BooleanBinding canMoveDown() {
        return Bindings.createBooleanBinding(
                () -> selectedIndex.get() >= 0 && selectedIndex.get() < steps.size() - 1, selectedIndex, steps);
    }

    // ----- File & format -----

    public void openFile(Path file) {
        track(context.tasks()
                .submit((progress, cancellation) -> useCases.open().execute(file), this::applySource, this::showError));
    }

    // ----- Pipeline -----

    public void addSplit() {
        List<String> columns = columnsBefore(steps.size());
        addStep(SplitStep.create(columns.isEmpty() ? "" : columns.getFirst()));
    }

    public void addMerge() {
        addStep(MergeStep.create(Messages.get("json.csv.merge.defaultName")));
    }

    public void addArrange() {
        addStep(ArrangeStep.create(columnsBefore(steps.size())));
    }

    public void select(int index) {
        if (index != selectedIndex.get() && index < steps.size()) {
            selectedIndex.set(index);
            bumpEditor();
        }
    }

    public Optional<PipelineStep> selectedStep() {
        int index = selectedIndex.get();
        return index >= 0 && index < steps.size() ? Optional.of(steps.get(index)) : Optional.empty();
    }

    /** Dipanggil editor langkah terpilih; editor tidak dibangun ulang supaya fokus ketik tidak hilang. */
    public void updateSelected(PipelineStep step) {
        int index = selectedIndex.get();
        if (index < 0 || index >= steps.size()) {
            return;
        }
        editingSelectedStep = true;
        try {
            steps.set(index, step);
        } finally {
            editingSelectedStep = false;
        }
    }

    public void setEnabled(int index, boolean enabled) {
        if (index >= 0 && index < steps.size() && steps.get(index).enabled() != enabled) {
            steps.set(index, steps.get(index).withEnabled(enabled));
        }
    }

    public void moveSelected(int offset) {
        int from = selectedIndex.get();
        int to = from + offset;
        if (from < 0 || to < 0 || to >= steps.size()) {
            return;
        }
        PipelineStep step = steps.remove(from);
        steps.add(to, step);
        selectedIndex.set(to);
        bumpEditor();
    }

    public void removeSelected() {
        int index = selectedIndex.get();
        if (index < 0 || index >= steps.size()) {
            return;
        }
        steps.remove(index);
        selectedIndex.set(Math.min(index, steps.size() - 1));
        bumpEditor();
    }

    /** Kolom yang tersedia untuk langkah ke-{@code index} (mulai 0). */
    public List<String> columnsBefore(int index) {
        CsvSample current = sample.get();
        return current == null ? List.of() : ColumnPipeline.columnsBefore(current.columns(), steps, index);
    }

    // ----- Ekspor -----

    public void export() {
        CsvSource current = source.get();
        if (current == null || sample.get() == null) {
            return;
        }
        ExportRequest request = new ExportRequest(
                current, inputFormat(), mismatchPolicy.get(), List.copyOf(steps), outputOptions(), outputFile());
        result.set(null);
        exportTask = track(context.tasks()
                .submit(
                        (progress, cancellation) -> useCases.export().execute(request, progress, cancellation),
                        result::set,
                        this::showError));
    }

    public void download() {
        withResult(output -> context.downloads().download(output, source.get().resultFileName()));
    }

    public void saveAs() {
        withResult(output -> context.downloads().saveAs(output, source.get().resultFileName()));
    }

    // ----- Ringkasan teks -----

    public String fileSummary(CsvSource current) {
        return Messages.format("json.csv.file.summary", current.fileName(), ByteSizeFormat.format(current.sizeBytes()));
    }

    public String previewSummary(PipelinePreview current) {
        CsvSample loaded = sample.get();
        boolean hasMore = loaded != null && loaded.hasMore();
        return Messages.format(
                hasMore ? "json.csv.preview.summary.more" : "json.csv.preview.summary",
                current.table().rows().size(),
                current.table().columns().size());
    }

    public String resultSummary(ExportResult exported) {
        return Messages.format(
                "json.csv.result.summary",
                exported.rowsWritten(),
                exported.columnCount(),
                ByteSizeFormat.format(exported.outputBytes()),
                exported.duration().toNanos() / NANOS_PER_SECOND);
    }

    public void dispose() {
        previewDebounce.stop();
        cancel(loadTask);
        cancel(previewTask);
        cancel(exportTask);
        tempFiles.close();
    }

    // ----- Internal -----

    private void listenToInputFormat() {
        for (ObservableValue<?> property : List.of(delimiter, encoding, hasHeader)) {
            property.addListener((observable, previous, current) -> {
                if (!applyingDetectedFormat) {
                    reloadSample();
                }
            });
        }
        // Default "tulis header" mengikuti input; user tetap bisa mengubahnya setelah itu
        hasHeader.addListener((observable, previous, current) -> includeHeader.set(current));
        mismatchPolicy.addListener((observable, previous, current) -> schedulePreview());
    }

    private void listenToOutputOptions() {
        delimiter.addListener((observable, previous, current) -> applyDefaultOutputEncoding());
        outputDelimiter.addListener((observable, previous, current) -> applyDefaultOutputEncoding());
        for (ObservableValue<?> property :
                List.of(outputDelimiter, outputEncoding, quote, lineEnding, includeHeader, neutralizeFormulas)) {
            property.addListener((observable, previous, current) -> result.set(null));
        }
    }

    /** BOM jadi default saat pemisah hasil {@code ;}; user tetap bisa menggantinya setelah itu. */
    private void applyDefaultOutputEncoding() {
        outputEncoding.set(
                OutputOptions.defaultEncodingFor(outputDelimiter.get().resolve(delimiter.get())));
    }

    /** Langkah pipeline sengaja dipertahankan saat ganti file, supaya aturan bisa dipakai ulang untuk file sejenis. */
    private void applySource(CsvSource opened) {
        InputFormat detected = opened.detected();
        applyingDetectedFormat = true;
        try {
            delimiter.set(detected.delimiter());
            encoding.set(detected.encoding());
            hasHeader.set(detected.hasHeader());
        } finally {
            applyingDetectedFormat = false;
        }
        source.set(opened);
        applyDefaultOutputEncoding();
        reloadSample();
    }

    private void reloadSample() {
        CsvSource current = source.get();
        if (current == null) {
            return;
        }
        InputFormat format = inputFormat();
        cancel(loadTask);
        sample.set(null);
        inputTable.set(null);
        preview.set(null);
        previewError.set("");
        result.set(null);
        loadTask = track(context.tasks()
                .submit(
                        (progress, cancellation) -> {
                            CsvSample loaded = useCases.loadSample().execute(current, format, cancellation);
                            return new LoadedSample(loaded, loaded.inputTable());
                        },
                        loaded -> {
                            sample.set(loaded.sample());
                            inputTable.set(loaded.table());
                            bumpEditor();
                            refreshPreview();
                        },
                        this::showError));
    }

    private void schedulePreview() {
        result.set(null);
        previewDebounce.trigger();
    }

    private void refreshPreview() {
        CsvSample current = sample.get();
        long generation = ++previewGeneration;
        cancel(previewTask);
        if (current == null) {
            preview.set(null);
            return;
        }
        List<PipelineStep> snapshot = List.copyOf(steps);
        MismatchPolicy policy = mismatchPolicy.get();
        previewTask = context.tasks()
                .submit(
                        (progress, cancellation) -> useCases.preview().execute(current, policy, snapshot, cancellation),
                        computed -> {
                            if (generation == previewGeneration) {
                                preview.set(computed);
                                previewError.set("");
                            }
                        },
                        error -> {
                            if (generation == previewGeneration) {
                                showPreviewFailure(error);
                            }
                        });
    }

    /** Aturan yang salah ditampilkan inline di pratinjau, bukan dialog, karena terjadi sambil user mengetik. */
    private void showPreviewFailure(Throwable error) {
        preview.set(null);
        switch (error) {
            case InvalidStepException invalid -> previewError.set(CsvErrorMessages.describe(invalid));
            case RegexSplitException regex -> previewError.set(CsvErrorMessages.describe(regex));
            default -> showError(error);
        }
    }

    private InputFormat inputFormat() {
        return new InputFormat(delimiter.get(), encoding.get(), hasHeader.get());
    }

    private OutputOptions outputOptions() {
        return new OutputOptions(
                outputDelimiter.get(),
                outputEncoding.get(),
                quote.get(),
                lineEnding.get(),
                includeHeader.get(),
                neutralizeFormulas.get());
    }

    private void addStep(PipelineStep step) {
        steps.add(step);
        selectedIndex.set(steps.size() - 1);
        bumpEditor();
    }

    private void bumpEditor() {
        editorRevision.set(editorRevision.get() + 1);
    }

    private void withResult(Consumer<Path> action) {
        ExportResult exported = result.get();
        if (exported != null && source.get() != null) {
            action.accept(exported.output());
        }
    }

    private Path outputFile() {
        if (outputFile == null) {
            outputFile = tempFiles.createFile(".csv");
        }
        return outputFile;
    }

    private void showError(Throwable error) {
        CsvErrorMessages.show(context.owner(), error);
    }

    private static void cancel(RunningTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    private RunningTask track(RunningTask task) {
        busy.set(true);
        currentTask.set(task);
        // Task lama yang selesai/dibatalkan belakangan tidak boleh mematikan status busy task baru
        task.runningProperty().addListener((observable, wasRunning, running) -> {
            if (currentTask.get() == task) {
                busy.set(running);
            }
        });
        return task;
    }

    private record LoadedSample(CsvSample sample, TablePreview table) {}
}
