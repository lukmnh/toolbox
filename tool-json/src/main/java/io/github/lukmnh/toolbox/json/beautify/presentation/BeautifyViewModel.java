package io.github.lukmnh.toolbox.json.beautify.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.json.beautify.application.FormatJsonUseCase;
import io.github.lukmnh.toolbox.json.beautify.application.LoadJsonFileUseCase;
import io.github.lukmnh.toolbox.json.beautify.application.ReadOutputUseCase;
import io.github.lukmnh.toolbox.json.beautify.domain.BeautifyAction;
import io.github.lukmnh.toolbox.json.beautify.domain.BeautifyLimits;
import io.github.lukmnh.toolbox.json.beautify.domain.FormatOptions;
import io.github.lukmnh.toolbox.json.beautify.domain.FormatResult;
import io.github.lukmnh.toolbox.json.beautify.domain.Indent;
import io.github.lukmnh.toolbox.json.beautify.domain.JsonInput;
import io.github.lukmnh.toolbox.json.beautify.domain.TextPreview;
import io.github.lukmnh.toolbox.json.shared.domain.MalformedJsonException;
import io.github.lukmnh.toolbox.json.shared.presentation.JsonErrorMessages;
import java.nio.file.Path;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** State dan aksi JSON Beautify. */
public final class BeautifyViewModel {

    /** Nama dasar file hasil untuk input yang di-paste. */
    private static final String PASTED_BASE_NAME = "pasted";

    private static final double NANOS_PER_SECOND = 1e9;

    private final LoadJsonFileUseCase loadUseCase;
    private final FormatJsonUseCase formatUseCase;
    private final ReadOutputUseCase readUseCase;
    private final ToolContext context;
    private final TempFiles tempFiles;

    private final StringProperty text = new SimpleStringProperty("");
    private final ObjectProperty<JsonInput.FileInput> file = new SimpleObjectProperty<>();
    private final ObjectProperty<Indent> indent = new SimpleObjectProperty<>(Indent.DEFAULT);
    private final BooleanProperty sortKeys = new SimpleBooleanProperty();
    private final ObjectProperty<FormatResult> result = new SimpleObjectProperty<>();
    private final ObjectProperty<TextPreview> preview = new SimpleObjectProperty<>();
    private final ObjectProperty<MalformedJsonException> error = new SimpleObjectProperty<>();
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final ObjectProperty<RunningTask> currentTask = new SimpleObjectProperty<>();
    private RunningTask runningTask;
    /** Satu temp file dipakai ulang supaya hasil berulang dari file ratusan MB tidak menumpuk di disk. */
    private Path outputFile;

    public BeautifyViewModel(
            LoadJsonFileUseCase loadUseCase,
            FormatJsonUseCase formatUseCase,
            ReadOutputUseCase readUseCase,
            ToolContext context,
            TempFiles tempFiles) {
        this.loadUseCase = loadUseCase;
        this.formatUseCase = formatUseCase;
        this.readUseCase = readUseCase;
        this.context = context;
        this.tempFiles = tempFiles;
        text.addListener((observable, previous, current) -> clearResult());
        file.addListener((observable, previous, current) -> {
            clearResult();
            if (current != null && !BeautifyLimits.canSortKeys(current.sizeBytes())) {
                sortKeys.set(false);
            }
        });
        indent.addListener((observable, previous, current) -> clearResult());
        sortKeys.addListener((observable, previous, current) -> clearResult());
    }

    public StringProperty textProperty() {
        return text;
    }

    public ReadOnlyObjectProperty<JsonInput.FileInput> fileProperty() {
        return file;
    }

    public ObjectProperty<Indent> indentProperty() {
        return indent;
    }

    public BooleanProperty sortKeysProperty() {
        return sortKeys;
    }

    public ReadOnlyObjectProperty<FormatResult> resultProperty() {
        return result;
    }

    public ReadOnlyObjectProperty<TextPreview> previewProperty() {
        return preview;
    }

    public ReadOnlyObjectProperty<MalformedJsonException> errorProperty() {
        return error;
    }

    public ReadOnlyObjectProperty<RunningTask> currentTaskProperty() {
        return currentTask;
    }

    public BooleanBinding canRun() {
        return busy.not().and(file.isNotNull().or(text.isNotEmpty()));
    }

    /** Sort keys nonaktif untuk file di atas 50 MB (BR-JS-2). */
    public BooleanBinding sortKeysAvailable() {
        return Bindings.createBooleanBinding(
                () -> file.get() == null
                        || BeautifyLimits.canSortKeys(file.get().sizeBytes()),
                file);
    }

    public BooleanBinding hasOutput() {
        return Bindings.createBooleanBinding(
                () -> result.get() != null && result.get().outputFile().isPresent(), result);
    }

    /** Copy hanya untuk hasil ≤ 10 MB (BR-JS-4). */
    public BooleanBinding canCopy() {
        return Bindings.createBooleanBinding(
                        () -> result.get() != null
                                && result.get().outputFile().isPresent()
                                && BeautifyLimits.canCopy(result.get().outputBytes()),
                        result)
                .and(busy.not());
    }

    public void openFile(Path candidate) {
        track(context.tasks()
                .submit((progress, cancellation) -> loadUseCase.execute(candidate), file::set, this::showError));
    }

    public void clearFile() {
        file.set(null);
    }

    public void run(BeautifyAction action) {
        JsonInput input = file.get() != null ? file.get() : new JsonInput.Text(text.get());
        boolean sort = sortKeys.get() && action != BeautifyAction.VALIDATE;
        if (sort && file.get() == null && !BeautifyLimits.canSortKeys(input.sizeBytes())) {
            context.notifier().warning(Messages.get("json.beautify.sortKeys.tooLarge"));
            return;
        }
        FormatOptions options = new FormatOptions(action, indent.get(), sort);
        Path output = action.writesOutput() ? outputFile() : null;
        clearResult();
        track(context.tasks()
                .submit(
                        (progress, cancellation) -> {
                            FormatResult formatted =
                                    formatUseCase.execute(input, options, output, progress, cancellation);
                            TextPreview outputPreview = formatted
                                    .outputFile()
                                    .map(readUseCase::preview)
                                    .orElse(null);
                            return new Outcome(formatted, outputPreview);
                        },
                        outcome -> {
                            result.set(outcome.result());
                            preview.set(outcome.preview());
                        },
                        this::showFailure));
    }

    /** Baca hasil lengkap di background, lalu serahkan ke {@code clipboard} di FX thread. */
    public void copy(Consumer<String> clipboard) {
        FormatResult current = result.get();
        if (current == null) {
            return;
        }
        track(context.tasks()
                .submit(
                        (progress, cancellation) -> readUseCase.fullText(current),
                        content -> {
                            clipboard.accept(content);
                            context.notifier().info(Messages.get("json.beautify.copied"));
                        },
                        this::showError));
    }

    public void download() {
        result.get().outputFile().ifPresent(output -> context.downloads().download(output, outputName()));
    }

    public void saveAs() {
        result.get().outputFile().ifPresent(output -> context.downloads().saveAs(output, outputName()));
    }

    public String fileSummary(JsonInput.FileInput input) {
        return Messages.format(
                "json.beautify.file.summary", input.file().getFileName(), ByteSizeFormat.format(input.sizeBytes()));
    }

    public String resultSummary(FormatResult formatted) {
        double seconds = formatted.duration().toNanos() / NANOS_PER_SECOND;
        if (formatted.action() == BeautifyAction.VALIDATE) {
            return Messages.format(
                    "json.beautify.valid",
                    formatted.stats().keyCount(),
                    formatted.stats().elementCount(),
                    seconds);
        }
        return Messages.format(
                "json.beautify.done",
                ByteSizeFormat.format(formatted.inputBytes()),
                ByteSizeFormat.format(formatted.outputBytes()),
                formatted.stats().keyCount(),
                formatted.stats().elementCount(),
                seconds);
    }

    public String errorHeader(MalformedJsonException malformed) {
        return JsonErrorMessages.header(malformed);
    }

    public String errorDetail(MalformedJsonException malformed) {
        return JsonErrorMessages.detail(malformed);
    }

    public void dispose() {
        if (runningTask != null) {
            runningTask.cancel();
        }
        tempFiles.close();
    }

    private String outputName() {
        String baseName = file.get() != null ? file.get().baseName() : PASTED_BASE_NAME;
        return result.get().action().outputName(baseName);
    }

    private Path outputFile() {
        if (outputFile == null) {
            outputFile = tempFiles.createFile(".json");
        }
        return outputFile;
    }

    private void clearResult() {
        result.set(null);
        preview.set(null);
        error.set(null);
    }

    private void showFailure(Throwable failure) {
        if (failure instanceof MalformedJsonException malformed) {
            error.set(malformed);
            return;
        }
        showError(failure);
    }

    private void showError(Throwable failure) {
        JsonErrorMessages.show(context.owner(), failure);
    }

    private void track(RunningTask task) {
        runningTask = task;
        busy.set(true);
        task.runningProperty().addListener((observable, wasRunning, running) -> busy.set(running));
        currentTask.set(task);
    }

    private record Outcome(FormatResult result, TextPreview preview) {}
}
