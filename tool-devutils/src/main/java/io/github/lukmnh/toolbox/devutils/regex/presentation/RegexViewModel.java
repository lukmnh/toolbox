package io.github.lukmnh.toolbox.devutils.regex.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.text.RegexTimeoutException;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.devutils.regex.application.RegexAnalysis;
import io.github.lukmnh.toolbox.devutils.regex.application.RegexRun;
import io.github.lukmnh.toolbox.devutils.regex.application.ReplaceResult;
import io.github.lukmnh.toolbox.devutils.regex.domain.LoadedTestText;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexFlag;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexFlavor;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexProblem;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexRequest;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexTooComplexException;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexToolException;
import io.github.lukmnh.toolbox.devutils.regex.domain.Span;
import io.github.lukmnh.toolbox.devutils.shared.presentation.ClipboardActions;
import io.github.lukmnh.toolbox.devutils.shared.presentation.Debounce;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.util.Duration;

/**
 * State dan aksi Regex Tester. Setiap perubahan pola, flag, teks uji, atau replacement memicu evaluasi live (debounce
 * 300 ms) di background; evaluasi sebelumnya dibatalkan supaya hasil lama tidak menimpa hasil baru.
 */
final class RegexViewModel {

    static final int NO_POSITION = -1;
    private static final Duration DEBOUNCE = Duration.millis(300);
    private static final java.time.Duration TIMEOUT = SecurityLimits.REGEX_TIMEOUT;

    private final RegexUseCases useCases;
    private final ToolContext context;
    private final Debounce debounce = new Debounce(DEBOUNCE, this::evaluate);

    private final ObjectProperty<RegexFlavor> flavor = new SimpleObjectProperty<>(RegexFlavor.JAVA);
    private final ObservableSet<RegexFlag> flags = FXCollections.observableSet(EnumSet.noneOf(RegexFlag.class));
    private final StringProperty pattern = new SimpleStringProperty("");
    private final StringProperty testText = new SimpleStringProperty("");
    private final StringProperty replacement = new SimpleStringProperty("");

    private final ObjectProperty<Evaluation> evaluation = new SimpleObjectProperty<>();
    private final StringProperty patternError = new SimpleStringProperty("");
    private final IntegerProperty errorPosition = new SimpleIntegerProperty(NO_POSITION);
    private final StringProperty runWarning = new SimpleStringProperty("");
    private final StringProperty replaceError = new SimpleStringProperty("");
    private final ObjectProperty<Span> hoveredSpan = new SimpleObjectProperty<>();
    private RunningTask runningTask;

    RegexViewModel(RegexUseCases useCases, ToolContext context) {
        this.useCases = useCases;
        this.context = context;
        flavor.addListener((observable, previous, current) -> resetFlags(current));
        for (Observable source : List.<Observable>of(flavor, flags, pattern, testText, replacement)) {
            source.addListener(observable -> debounce.trigger());
        }
    }

    ObjectProperty<RegexFlavor> flavorProperty() {
        return flavor;
    }

    ObservableSet<RegexFlag> flags() {
        return flags;
    }

    StringProperty patternProperty() {
        return pattern;
    }

    StringProperty testTextProperty() {
        return testText;
    }

    StringProperty replacementProperty() {
        return replacement;
    }

    ReadOnlyObjectProperty<Evaluation> evaluationProperty() {
        return evaluation;
    }

    ReadOnlyStringProperty patternErrorProperty() {
        return patternError;
    }

    ReadOnlyIntegerProperty errorPositionProperty() {
        return errorPosition;
    }

    ReadOnlyStringProperty runWarningProperty() {
        return runWarning;
    }

    ReadOnlyStringProperty replaceErrorProperty() {
        return replaceError;
    }

    ObjectProperty<Span> hoveredSpanProperty() {
        return hoveredSpan;
    }

    void toggleFlag(RegexFlag flag, boolean enabled) {
        if (enabled) {
            flags.add(flag);
        } else {
            flags.remove(flag);
        }
    }

    /** Masalah parser ditampilkan untuk flavor terjemahan; flavor native memakai error engine sebagai acuan. */
    List<RegexProblem> visibleProblems() {
        Evaluation current = evaluation.get();
        if (current == null || !flavor.get().translated()) {
            return List.of();
        }
        return current.analysis().parsed().problems();
    }

    void loadFile(Path file) {
        context.tasks()
                .submit(
                        (progress, cancellation) -> useCases.load().execute(file),
                        (LoadedTestText loaded) -> testText.set(loaded.text()),
                        this::showLoadError);
    }

    void copyCode() {
        ClipboardActions.copy(CodeLiteral.of(flavor.get(), pattern.get(), Set.copyOf(flags)), context.notifier());
    }

    void copyReplaceOutput() {
        Evaluation current = evaluation.get();
        if (current != null) {
            current.replace().ifPresent(result -> ClipboardActions.copy(result.output(), context.notifier()));
        }
    }

    void dispose() {
        debounce.stop();
        cancelRunning();
    }

    /** JS default flag {@code g} (seperti regex101); flavor lain tanpa flag. */
    static Set<RegexFlag> defaultFlags(RegexFlavor target) {
        return target == RegexFlavor.JAVASCRIPT ? Set.of(RegexFlag.JS_GLOBAL) : Set.of();
    }

    private void resetFlags(RegexFlavor target) {
        flags.clear();
        flags.addAll(defaultFlags(target));
    }

    private void evaluate() {
        cancelRunning();
        if (pattern.get().isEmpty()) {
            clearResults();
            return;
        }
        RegexRequest request = new RegexRequest(pattern.get(), flavor.get(), Set.copyOf(flags));
        String text = testText.get();
        String template = replacement.get();
        runningTask = context.tasks()
                .submit(
                        (progress, cancellation) -> compute(request, text, template),
                        this::show,
                        error -> ErrorDialogs.showUnexpected(context.owner(), error));
    }

    /** Berjalan di background; kegagalan yang sudah dikenal disimpan untuk ditampilkan inline. */
    private Evaluation compute(RegexRequest request, String text, String template) {
        RegexAnalysis analysis = useCases.analyze().execute(request);
        Optional<RegexRun> run = Optional.empty();
        try {
            run = Optional.of(useCases.run().execute(request, text, TIMEOUT));
        } catch (RegexToolException | RegexTimeoutException e) {
            // Run gagal: replace pasti gagal karena sebab yang sama, jadi tidak dijalankan
            return new Evaluation(text, analysis, run, Optional.of(e), Optional.empty(), Optional.empty());
        }
        Optional<ReplaceResult> replaced = Optional.empty();
        Optional<RuntimeException> replaceFailure = Optional.empty();
        try {
            replaced = Optional.of(useCases.replace().execute(request, text, template, TIMEOUT));
        } catch (RegexToolException | RegexTimeoutException e) {
            replaceFailure = Optional.of(e);
        }
        return new Evaluation(text, analysis, run, Optional.empty(), replaced, replaceFailure);
    }

    private void show(Evaluation result) {
        runningTask = null;
        patternError.set("");
        errorPosition.set(NO_POSITION);
        runWarning.set("");
        replaceError.set("");
        result.runFailure().ifPresent(this::showRunFailure);
        result.replaceFailure().ifPresent(failure -> replaceError.set(describe(failure)));
        evaluation.set(result);
    }

    private void showRunFailure(RuntimeException failure) {
        switch (failure) {
            case RegexTimeoutException e -> runWarning.set(Messages.get("devutils.regex.error.timeout"));
            // Bukan salah pola, tapi kombinasi pola + teks terlalu berat: tampil sebagai peringatan di teks uji
            case RegexTooComplexException e -> runWarning.set(RegexMessages.describe(e, flavor.get()));
            case RegexToolException e -> {
                patternError.set(RegexMessages.describe(e, flavor.get()));
                errorPosition.set(RegexMessages.position(e));
            }
            default -> throw failure;
        }
    }

    private String describe(RuntimeException failure) {
        return switch (failure) {
            case RegexTimeoutException e -> Messages.get("devutils.regex.error.timeout");
            case RegexToolException e -> RegexMessages.describe(e, flavor.get());
            default -> throw failure;
        };
    }

    private void showLoadError(Throwable error) {
        if (error instanceof RegexToolException toolError) {
            ErrorDialogs.show(
                    context.owner(),
                    Messages.get("devutils.regex.error.load.header"),
                    RegexMessages.describe(toolError, flavor.get()));
            return;
        }
        ErrorDialogs.showUnexpected(context.owner(), error);
    }

    private void clearResults() {
        evaluation.set(null);
        patternError.set("");
        errorPosition.set(NO_POSITION);
        runWarning.set("");
        replaceError.set("");
    }

    private void cancelRunning() {
        if (runningTask != null) {
            runningTask.cancel();
            runningTask = null;
        }
    }
}
