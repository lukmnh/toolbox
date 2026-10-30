package io.github.lukmnh.toolbox.productivity.text.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.common.ui.ClipboardActions;
import io.github.lukmnh.toolbox.common.ui.Debounce;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.productivity.shared.domain.CaseStyle;
import io.github.lukmnh.toolbox.productivity.shared.domain.IndonesianNumbers;
import io.github.lukmnh.toolbox.productivity.text.domain.TextHistory;
import io.github.lukmnh.toolbox.productivity.text.domain.TextSize;
import io.github.lukmnh.toolbox.productivity.text.domain.TextStatistics;
import io.github.lukmnh.toolbox.productivity.text.domain.TextTransformation;
import io.github.lukmnh.toolbox.productivity.text.domain.TidyOperation;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Duration;

/** State dan aksi Text Tools. Semua method dipanggil dari FX thread. */
final class TextViewModel {

    static final long MAX_BYTES = SecurityLimits.TEXT_TOOLS_MAX_BYTES;

    private static final Duration STATS_DELAY = Duration.millis(150);
    private static final String DOWNLOAD_NAME = "teks.txt";
    private static final String TEXT_SUFFIX = ".txt";

    private final TextUseCases useCases;
    private final ToolContext context;
    private final TempFiles tempFiles;
    private final TextHistory history = new TextHistory();

    private final StringProperty text = new SimpleStringProperty("");
    private final BooleanProperty ignoreStopWords = new SimpleBooleanProperty(true);
    private final ObjectProperty<TextStatistics> statistics = new SimpleObjectProperty<>(TextStatistics.EMPTY);
    private final BooleanProperty analyzing = new SimpleBooleanProperty();
    private final StringProperty characterLimit = new SimpleStringProperty("");
    private final ObjectProperty<LimitIndicator> limitIndicator = new SimpleObjectProperty<>(LimitIndicator.HIDDEN);
    private final ObjectProperty<CaseStyle> caseStyle = new SimpleObjectProperty<>(CaseStyle.UPPER);
    private final BooleanProperty indonesianTitle = new SimpleBooleanProperty(true);
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final BooleanProperty canUndo = new SimpleBooleanProperty();
    private final ObjectProperty<RunningTask> currentTask = new SimpleObjectProperty<>();
    private final Debounce debounce = new Debounce(STATS_DELAY, this::analyze);

    private RunningTask statsTask;
    private RunningTask actionTask;
    /** Satu temp file dipakai ulang supaya download berulang tidak menumpuk di disk. */
    private Path outputFile;

    TextViewModel(TextUseCases useCases, ToolContext context, TempFiles tempFiles) {
        this.useCases = useCases;
        this.context = context;
        this.tempFiles = tempFiles;
        text.addListener((observable, previous, current) -> debounce.trigger());
        ignoreStopWords.addListener((observable, previous, current) -> debounce.trigger());
        statistics.addListener((observable, previous, current) -> updateLimitIndicator());
        characterLimit.addListener((observable, previous, current) -> updateLimitIndicator());
    }

    StringProperty textProperty() {
        return text;
    }

    BooleanProperty ignoreStopWordsProperty() {
        return ignoreStopWords;
    }

    ReadOnlyObjectProperty<TextStatistics> statisticsProperty() {
        return statistics;
    }

    ReadOnlyBooleanProperty analyzingProperty() {
        return analyzing;
    }

    StringProperty characterLimitProperty() {
        return characterLimit;
    }

    ReadOnlyObjectProperty<LimitIndicator> limitIndicatorProperty() {
        return limitIndicator;
    }

    ObjectProperty<CaseStyle> caseStyleProperty() {
        return caseStyle;
    }

    BooleanProperty indonesianTitleProperty() {
        return indonesianTitle;
    }

    ReadOnlyBooleanProperty busyProperty() {
        return busy;
    }

    ReadOnlyBooleanProperty canUndoProperty() {
        return canUndo;
    }

    ReadOnlyObjectProperty<RunningTask> currentTaskProperty() {
        return currentTask;
    }

    BooleanBinding canTransform() {
        return text.isNotEmpty().and(busy.not());
    }

    /**
     * Penjaga ukuran untuk editan user (ketik/paste). Teks hasil transformasi dicek terpisah di
     * {@link #replaceText}, jadi penolakan di sini hanya untuk input user.
     */
    boolean accepts(String candidate) {
        if (!TextSize.exceeds(candidate, MAX_BYTES)) {
            return true;
        }
        warnTooLarge();
        return false;
    }

    void applyCase() {
        transform(new TextTransformation.ChangeCase(caseStyle.get(), indonesianTitle.get()));
    }

    void tidy(TidyOperation operation) {
        transform(new TextTransformation.Tidy(operation));
    }

    void undo() {
        history.undo().ifPresent(text::set);
        canUndo.set(!history.isEmpty());
    }

    void clear() {
        replaceText(text.get(), "");
    }

    void loadFile(Path file) {
        track(context.tasks()
                .submit(
                        (progress, cancellation) -> useCases.load().execute(file),
                        loaded -> replaceText(text.get(), loaded.text()),
                        error -> TextErrorMessages.show(context.owner(), error)));
    }

    void copy() {
        ClipboardActions.copy(text.get(), context.notifier());
    }

    void download() {
        export(file -> context.downloads().download(file, DOWNLOAD_NAME));
    }

    void saveAs() {
        export(file -> context.downloads().saveAs(file, DOWNLOAD_NAME));
    }

    void dispose() {
        debounce.stop();
        cancel(statsTask);
        cancel(actionTask);
        tempFiles.close();
    }

    private void analyze() {
        cancel(statsTask);
        String snapshot = text.get();
        boolean ignore = ignoreStopWords.get();
        analyzing.set(true);
        statsTask = context.tasks()
                .submit(
                        (progress, cancellation) -> useCases.analyze().execute(snapshot, ignore, cancellation),
                        result -> {
                            analyzing.set(false);
                            statistics.set(result);
                        },
                        error -> {
                            analyzing.set(false);
                            TextErrorMessages.show(context.owner(), error);
                        });
    }

    /** Editor dikunci selama transformasi (lihat view), jadi {@code before} pasti masih teks yang tampil. */
    private void transform(TextTransformation transformation) {
        String before = text.get();
        if (before.isEmpty()) {
            return;
        }
        track(context.tasks()
                .submit(
                        (progress, cancellation) -> useCases.transform().execute(before, transformation, cancellation),
                        after -> replaceText(before, after),
                        error -> TextErrorMessages.show(context.owner(), error)));
    }

    /** Semua perubahan non-ketik lewat sini supaya bisa di-undo dan tetap di bawah batas ukuran. */
    private void replaceText(String before, String after) {
        if (after.equals(before)) {
            context.notifier().info(Messages.get("productivity.text.unchanged"));
            return;
        }
        // Ubah huruf bisa memperpanjang teks (contoh ß → SS), jadi hasil dicek ulang
        if (TextSize.exceeds(after, MAX_BYTES)) {
            warnTooLarge();
            return;
        }
        history.push(before);
        canUndo.set(true);
        text.set(after);
    }

    private void export(Consumer<Path> deliver) {
        String snapshot = text.get();
        if (snapshot.isEmpty()) {
            return;
        }
        Path target = outputFile();
        track(context.tasks()
                .submit(
                        (progress, cancellation) -> {
                            useCases.export().execute(snapshot, target);
                            return target;
                        },
                        deliver,
                        error -> TextErrorMessages.show(context.owner(), error)));
    }

    private Path outputFile() {
        if (outputFile == null) {
            outputFile = tempFiles.createFile(TEXT_SUFFIX);
        }
        return outputFile;
    }

    private void updateLimitIndicator() {
        if (characterLimit.get().isBlank()) {
            limitIndicator.set(LimitIndicator.HIDDEN);
            return;
        }
        Optional<Long> limit = parseLimit(characterLimit.get());
        if (limit.isEmpty()) {
            limitIndicator.set(new LimitIndicator(Messages.get("productivity.text.limit.invalid"), Tone.WARNING));
            return;
        }
        long remaining = limit.get() - statistics.get().characters();
        if (remaining >= 0) {
            limitIndicator.set(
                    new LimitIndicator(Messages.format("productivity.text.limit.remaining", remaining), Tone.SUCCESS));
            return;
        }
        limitIndicator.set(
                new LimitIndicator(Messages.format("productivity.text.limit.over", -remaining), Tone.DANGER));
    }

    /** Batas berupa bilangan bulat positif; format Indonesia {@code 1.000} diterima. */
    private static Optional<Long> parseLimit(String input) {
        Optional<BigDecimal> parsed = IndonesianNumbers.parse(input);
        if (parsed.isEmpty() || parsed.get().signum() <= 0) {
            return Optional.empty();
        }
        try {
            return Optional.of(parsed.get().longValueExact());
        } catch (ArithmeticException e) {
            // Pecahan atau melebihi long: bukan batas karakter yang valid
            return Optional.empty();
        }
    }

    private void warnTooLarge() {
        context.notifier()
                .warning(Messages.format("productivity.text.error.tooLarge", ByteSizeFormat.format(MAX_BYTES)));
    }

    private void track(RunningTask task) {
        actionTask = task;
        busy.set(true);
        task.runningProperty().addListener((observable, wasRunning, running) -> busy.set(running));
        currentTask.set(task);
    }

    private static void cancel(RunningTask task) {
        if (task != null) {
            task.cancel();
        }
    }
}
