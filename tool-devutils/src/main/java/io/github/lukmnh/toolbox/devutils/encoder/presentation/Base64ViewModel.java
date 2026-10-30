package io.github.lukmnh.toolbox.devutils.encoder.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.FileExtensions;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.common.ui.ClipboardActions;
import io.github.lukmnh.toolbox.common.ui.Debounce;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.devutils.encoder.domain.Base64Codec;
import io.github.lukmnh.toolbox.devutils.encoder.domain.Base64Limits;
import io.github.lukmnh.toolbox.devutils.encoder.domain.Base64Options;
import io.github.lukmnh.toolbox.devutils.encoder.domain.Base64Variant;
import io.github.lukmnh.toolbox.devutils.encoder.domain.DecodedFile;
import io.github.lukmnh.toolbox.devutils.encoder.domain.EncodedFile;
import io.github.lukmnh.toolbox.devutils.encoder.domain.EncoderException;
import io.github.lukmnh.toolbox.devutils.encoder.domain.EncoderIoException;
import io.github.lukmnh.toolbox.devutils.encoder.domain.FileEncodeOptions;
import io.github.lukmnh.toolbox.devutils.encoder.domain.ImageSize;
import io.github.lukmnh.toolbox.devutils.encoder.domain.TextCharset;
import io.github.lukmnh.toolbox.devutils.encoder.domain.TextConversion;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.util.Duration;

/** State dan aksi tab Base64: mode Teks (live) dan mode File (streaming di background). */
public final class Base64ViewModel {

    static final Set<String> BASE64_TEXT_EXTENSIONS = Set.of("txt", "b64");

    private static final Duration DEBOUNCE_DELAY = Duration.millis(200);
    private static final String ENCODED_SUFFIX = ".b64.txt";

    private final EncoderUseCases useCases;
    private final ToolContext context;
    private final TempFiles tempFiles;
    private final Debounce debounce = new Debounce(DEBOUNCE_DELAY, this::convertText);

    private final ObjectProperty<InputMode> mode = new SimpleObjectProperty<>(InputMode.TEXT);
    private final ObjectProperty<Direction> direction = new SimpleObjectProperty<>(Direction.ENCODE);
    private final ObjectProperty<Base64Variant> variant = new SimpleObjectProperty<>(Base64Variant.STANDARD);
    private final ObjectProperty<TextCharset> charset = new SimpleObjectProperty<>(TextCharset.UTF_8);
    private final BooleanProperty padding = new SimpleBooleanProperty(true);

    private final StringProperty input = new SimpleStringProperty("");
    private final StringProperty output = new SimpleStringProperty("");
    private final StringProperty stats = new SimpleStringProperty("");
    private final StringProperty textError = new SimpleStringProperty("");

    private final ObjectProperty<Path> sourceFile = new SimpleObjectProperty<>();
    private final StringProperty sourceInfo = new SimpleStringProperty("");
    private final StringProperty pastedBase64 = new SimpleStringProperty("");
    private final BooleanProperty dataUri = new SimpleBooleanProperty();
    private final ObjectProperty<FileResult> fileResult = new SimpleObjectProperty<>();
    private final StringProperty fileError = new SimpleStringProperty("");
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final ObjectProperty<RunningTask> currentTask = new SimpleObjectProperty<>();

    private RunningTask textTask;
    private RunningTask fileTask;
    /** Satu temp file per arah dipakai ulang supaya hasil berulang tidak menumpuk di disk. */
    private Path encodedOutput;

    private Path decodedOutput;

    public Base64ViewModel(EncoderUseCases useCases, ToolContext context, TempFiles tempFiles) {
        this.useCases = useCases;
        this.context = context;
        this.tempFiles = tempFiles;
        ChangeListener<Object> textChanged = (observable, previous, current) -> debounce.trigger();
        input.addListener(textChanged);
        direction.addListener(textChanged);
        variant.addListener(textChanged);
        charset.addListener(textChanged);
        padding.addListener(textChanged);
        ChangeListener<Object> fileOptionChanged = (observable, previous, current) -> clearFileResult();
        direction.addListener(fileOptionChanged);
        variant.addListener(fileOptionChanged);
        padding.addListener(fileOptionChanged);
        dataUri.addListener(fileOptionChanged);
        sourceFile.addListener(fileOptionChanged);
        pastedBase64.addListener(fileOptionChanged);
    }

    // ---------- Pilihan ----------

    ObjectProperty<InputMode> modeProperty() {
        return mode;
    }

    ObjectProperty<Direction> directionProperty() {
        return direction;
    }

    public ObjectProperty<Base64Variant> variantProperty() {
        return variant;
    }

    public ObjectProperty<TextCharset> charsetProperty() {
        return charset;
    }

    public BooleanProperty paddingProperty() {
        return padding;
    }

    /** Varian dan padding tidak berpengaruh saat decode (decode toleran) atau saat output data URI. */
    public BooleanBinding encodeOptionsDisabled() {
        return direction
                .isEqualTo(Direction.DECODE)
                .or(mode.isEqualTo(InputMode.FILE).and(dataUri));
    }

    // ---------- Mode Teks ----------

    public StringProperty inputProperty() {
        return input;
    }

    public ReadOnlyStringProperty outputProperty() {
        return output;
    }

    public ReadOnlyStringProperty statsProperty() {
        return stats;
    }

    public ReadOnlyStringProperty textErrorProperty() {
        return textError;
    }

    /** Output jadi input dan arah dibalik; hanya bila hasil saat ini valid. */
    public void swap() {
        if (output.get().isEmpty() || !textError.get().isEmpty()) {
            return;
        }
        String previousOutput = output.get();
        direction.set(direction.get().opposite());
        input.set(previousOutput);
    }

    public void copyOutput() {
        ClipboardActions.copy(output.get(), context.notifier());
    }

    private void convertText() {
        if (textTask != null) {
            textTask.cancel();
        }
        String text = input.get();
        if (text.isEmpty()) {
            showConversion("", "", "");
            return;
        }
        Direction currentDirection = direction.get();
        Base64Options options = new Base64Options(variant.get(), padding.get(), charset.get());
        textTask = context.tasks()
                .submit(
                        (progress, cancellation) -> currentDirection == Direction.ENCODE
                                ? Base64Codec.encode(text, options)
                                : Base64Codec.decode(text, options),
                        conversion -> showConversion(conversion.output(), statsText(conversion), ""),
                        failure -> handleFailure(failure, message -> showConversion("", "", message)));
    }

    private void showConversion(String result, String statistics, String error) {
        output.set(result);
        stats.set(statistics);
        textError.set(error);
    }

    private static String statsText(TextConversion conversion) {
        return Messages.format(
                "devutils.encoder.stats",
                conversion.inputChars(),
                conversion.inputBytes(),
                conversion.outputChars(),
                conversion.outputBytes());
    }

    // ---------- Mode File ----------

    public ReadOnlyObjectProperty<Path> sourceFileProperty() {
        return sourceFile;
    }

    public ReadOnlyStringProperty sourceInfoProperty() {
        return sourceInfo;
    }

    public StringProperty pastedBase64Property() {
        return pastedBase64;
    }

    public BooleanProperty dataUriProperty() {
        return dataUri;
    }

    public ReadOnlyObjectProperty<FileResult> fileResultProperty() {
        return fileResult;
    }

    public ReadOnlyStringProperty fileErrorProperty() {
        return fileError;
    }

    public ReadOnlyObjectProperty<RunningTask> currentTaskProperty() {
        return currentTask;
    }

    public BooleanBinding canRunFile() {
        return Bindings.createBooleanBinding(
                () -> !busy.get()
                        && (sourceFile.get() != null
                                || (direction.get() == Direction.DECODE
                                        && !pastedBase64.get().isBlank())),
                busy,
                sourceFile,
                direction,
                pastedBase64);
    }

    /** Copy hanya untuk hasil encode ≤ 10 MB. */
    public BooleanBinding canCopyFileResult() {
        return Bindings.createBooleanBinding(
                () -> fileResult.get() instanceof FileResult.Encoded encoded
                        && Base64Limits.canCopy(encoded.file().outputBytes())
                        && !busy.get(),
                fileResult,
                busy);
    }

    /** Decode hanya menerima file teks Base64; encode menerima file apa pun. */
    public void selectFile(Path file) {
        if (direction.get() == Direction.DECODE && !FileExtensions.hasExtension(file, BASE64_TEXT_EXTENSIONS)) {
            context.notifier().warning(Messages.format("devutils.drop.rejected", file.getFileName()));
            return;
        }
        try {
            long size = useCases.read().size(file);
            sourceInfo.set(
                    Messages.format("devutils.encoder.file.summary", file.getFileName(), ByteSizeFormat.format(size)));
            sourceFile.set(file);
        } catch (EncoderIoException e) {
            ErrorDialogs.show(
                    context.owner(), Messages.get("devutils.encoder.error.title"), EncoderErrorMessages.describe(e));
        }
    }

    public void clearFile() {
        sourceFile.set(null);
        sourceInfo.set("");
    }

    public void runFile() {
        clearFileResult();
        if (direction.get() == Direction.ENCODE) {
            encodeFile();
        } else {
            decodeFile();
        }
    }

    private void encodeFile() {
        Path source = sourceFile.get();
        FileEncodeOptions options = new FileEncodeOptions(variant.get(), padding.get(), dataUri.get());
        Path target = encodedOutput();
        String name = source.getFileName() + ENCODED_SUFFIX;
        trackFile(context.tasks()
                .submit(
                        (progress, cancellation) -> {
                            EncodedFile encoded =
                                    useCases.encode().execute(source, target, options, progress, cancellation);
                            return new FileResult.Encoded(
                                    encoded, useCases.read().preview(encoded), name);
                        },
                        fileResult::set,
                        failure -> handleFailure(failure, fileError::set)));
    }

    private void decodeFile() {
        Path source = sourceFile.get();
        String pasted = pastedBase64.get();
        Path target = decodedOutput();
        trackFile(context.tasks()
                .submit(
                        (progress, cancellation) -> {
                            DecodedFile decoded = source != null
                                    ? useCases.decode().fromFile(source, target, progress, cancellation)
                                    : useCases.decode().fromText(pasted, target);
                            return new FileResult.Decoded(decoded, imagePreview(decoded));
                        },
                        fileResult::set,
                        failure -> handleFailure(failure, fileError::set)));
    }

    /** Dicek dari header sebelum JavaFX men-decode piksel (Security Requirements #3). */
    private FileResult.ImagePreview imagePreview(DecodedFile decoded) {
        Optional<ImageSize> size = useCases.read().imageSize(decoded);
        if (size.isEmpty()) {
            return new FileResult.ImagePreview.None();
        }
        boolean allowed = size.get().fitsWithin(SecurityLimits.IMAGE_MAX_DIMENSION)
                && decoded.outputBytes() <= SecurityLimits.IMAGE_MAX_FILE_BYTES;
        return allowed
                ? new FileResult.ImagePreview.Allowed(size.get())
                : new FileResult.ImagePreview.TooLarge(size.get());
    }

    public String resultSummary(FileResult result) {
        return switch (result) {
            case FileResult.Encoded encoded ->
                Messages.format(
                        "devutils.encoder.file.encoded",
                        ByteSizeFormat.format(encoded.file().inputBytes()),
                        ByteSizeFormat.format(encoded.file().outputBytes()));
            case FileResult.Decoded decoded ->
                Messages.format(
                        "devutils.encoder.file.decoded",
                        ByteSizeFormat.format(decoded.file().outputBytes()),
                        decoded.file().type().extension().toUpperCase(Locale.ROOT));
        };
    }

    /** Baca hasil utuh di background, lalu copy di FX thread. */
    public void copyFileResult() {
        if (!(fileResult.get() instanceof FileResult.Encoded encoded)) {
            return;
        }
        trackFile(context.tasks()
                .submit(
                        (progress, cancellation) -> useCases.read().fullText(encoded.file()),
                        text -> ClipboardActions.copy(text, context.notifier()),
                        failure -> handleFailure(failure, fileError::set)));
    }

    public void download() {
        FileResult result = fileResult.get();
        if (result != null) {
            context.downloads().download(result.output(), result.suggestedName());
        }
    }

    public void saveAs() {
        FileResult result = fileResult.get();
        if (result != null) {
            context.downloads().saveAs(result.output(), result.suggestedName());
        }
    }

    public void dispose() {
        debounce.stop();
        if (textTask != null) {
            textTask.cancel();
        }
        if (fileTask != null) {
            fileTask.cancel();
        }
        tempFiles.close();
    }

    private void clearFileResult() {
        fileResult.set(null);
        fileError.set("");
    }

    private Path encodedOutput() {
        if (encodedOutput == null) {
            encodedOutput = tempFiles.createFile(".b64.txt");
        }
        return encodedOutput;
    }

    private Path decodedOutput() {
        if (decodedOutput == null) {
            decodedOutput = tempFiles.createFile(".bin");
        }
        return decodedOutput;
    }

    /** Error domain tampil inline di dekat input; selain itu dialog error generik (dan dicatat ke log). */
    private void handleFailure(Throwable failure, Consumer<String> inline) {
        if (failure instanceof EncoderException domain) {
            inline.accept(EncoderErrorMessages.describe(domain));
            return;
        }
        inline.accept("");
        ErrorDialogs.showUnexpected(context.owner(), failure);
    }

    private void trackFile(RunningTask task) {
        fileTask = task;
        busy.set(true);
        task.runningProperty().addListener((observable, wasRunning, running) -> busy.set(running));
        currentTask.set(task);
    }
}
