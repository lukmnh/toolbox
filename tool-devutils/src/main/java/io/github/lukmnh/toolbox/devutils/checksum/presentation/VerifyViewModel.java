package io.github.lukmnh.toolbox.devutils.checksum.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumAlgorithm;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumException;
import io.github.lukmnh.toolbox.devutils.checksum.domain.FileVerification;
import io.github.lukmnh.toolbox.devutils.checksum.domain.SumsVerification;
import io.github.lukmnh.toolbox.devutils.checksum.domain.VerifyStatus;
import java.nio.file.Path;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/** State dan aksi tab Verifikasi: (a) satu file + checksum, (b) file daftar {@code *SUMS}. */
final class VerifyViewModel {

    private final ChecksumUseCases useCases;
    private final ToolContext context;

    private final ObjectProperty<Path> file = new SimpleObjectProperty<>();
    private final StringProperty expected = new SimpleStringProperty("");
    private final ObjectProperty<AlgorithmChoice> fileChoice = new SimpleObjectProperty<>(AlgorithmChoice.AUTO);
    private final ObjectProperty<FileVerification> fileResult = new SimpleObjectProperty<>();
    private final StringProperty fileError = new SimpleStringProperty("");

    private final ObjectProperty<Path> sumsFile = new SimpleObjectProperty<>();
    private final ObjectProperty<AlgorithmChoice> sumsChoice = new SimpleObjectProperty<>(AlgorithmChoice.AUTO);
    private final ObservableList<SumsVerification> sumsResults = FXCollections.observableArrayList();
    private final StringProperty sumsError = new SimpleStringProperty("");

    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final ObjectProperty<RunningTask> currentTask = new SimpleObjectProperty<>();
    private RunningTask runningTask;

    VerifyViewModel(ChecksumUseCases useCases, ToolContext context) {
        this.useCases = useCases;
        this.context = context;
        file.addListener((observable, previous, current) -> clearFileResult());
        expected.addListener((observable, previous, current) -> clearFileResult());
        fileChoice.addListener((observable, previous, current) -> clearFileResult());
        sumsChoice.addListener((observable, previous, current) -> clearSumsResult());
    }

    ObjectProperty<Path> fileProperty() {
        return file;
    }

    StringProperty expectedProperty() {
        return expected;
    }

    ObjectProperty<AlgorithmChoice> fileChoiceProperty() {
        return fileChoice;
    }

    ReadOnlyObjectProperty<FileVerification> fileResultProperty() {
        return fileResult;
    }

    ReadOnlyStringProperty fileErrorProperty() {
        return fileError;
    }

    ReadOnlyObjectProperty<Path> sumsFileProperty() {
        return sumsFile;
    }

    ObjectProperty<AlgorithmChoice> sumsChoiceProperty() {
        return sumsChoice;
    }

    ObservableList<SumsVerification> sumsResults() {
        return sumsResults;
    }

    ReadOnlyStringProperty sumsErrorProperty() {
        return sumsError;
    }

    ReadOnlyObjectProperty<RunningTask> currentTaskProperty() {
        return currentTask;
    }

    BooleanBinding canVerifyFile() {
        return busy.not()
                .and(file.isNotNull())
                .and(Bindings.createBooleanBinding(() -> !expected.get().isBlank(), expected));
    }

    BooleanBinding idle() {
        return busy.not();
    }

    /** Teks badge algoritma terdeteksi; kosong bila algoritma dipilih manual atau input masih kosong. */
    StringBinding detectedAlgorithm() {
        return Bindings.createStringBinding(
                () -> {
                    if (fileChoice.get() != AlgorithmChoice.AUTO
                            || expected.get().isBlank()) {
                        return "";
                    }
                    return ChecksumAlgorithm.detect(expected.get())
                            .map(algorithm -> Messages.format("devutils.checksum.detected", algorithm.label()))
                            .orElseGet(() -> Messages.get("devutils.checksum.detected.unknown"));
                },
                expected,
                fileChoice);
    }

    void verifyFile() {
        Path target = file.get();
        String checksum = expected.get();
        AlgorithmChoice choice = fileChoice.get();
        clearFileResult();
        track(context.tasks()
                .submit(
                        (progress, cancellation) -> useCases.verifyFile()
                                .execute(target, checksum, choice.algorithm(), progress, cancellation),
                        fileResult::set,
                        error -> showInline(error, fileError::set)));
    }

    void verifySums(Path candidate) {
        sumsFile.set(candidate);
        AlgorithmChoice choice = sumsChoice.get();
        clearSumsResult();
        track(context.tasks()
                .submit(
                        (progress, cancellation) ->
                                useCases.verifySums().execute(candidate, choice.algorithm(), progress, cancellation),
                        sumsResults::setAll,
                        error -> showInline(error, sumsError::set)));
    }

    /** Ulangi verifikasi file daftar terakhir (misalnya setelah algoritma diganti). */
    void reverifySums() {
        if (sumsFile.get() != null) {
            verifySums(sumsFile.get());
        }
    }

    long count(VerifyStatus status) {
        return sumsResults.stream().filter(row -> row.status() == status).count();
    }

    void dispose() {
        if (runningTask != null) {
            runningTask.cancel();
        }
    }

    /** Error domain tampil inline di card; error tak terduga tetap lewat dialog + log. */
    private void showInline(Throwable error, Consumer<String> target) {
        if (error instanceof ChecksumException checksumError) {
            target.accept(ChecksumErrorMessages.describe(checksumError));
            return;
        }
        ErrorDialogs.showUnexpected(context.owner(), error);
    }

    private void clearFileResult() {
        fileResult.set(null);
        fileError.set("");
    }

    private void clearSumsResult() {
        sumsResults.clear();
        sumsError.set("");
    }

    private void track(RunningTask task) {
        runningTask = task;
        busy.set(true);
        task.runningProperty().addListener((observable, wasRunning, running) -> busy.set(running));
        currentTask.set(task);
    }
}
