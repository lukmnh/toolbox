package io.github.lukmnh.toolbox.pdf.split.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.FileExtensions;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.pdf.shared.application.InspectPdfUseCase;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfDocumentInfo;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import io.github.lukmnh.toolbox.pdf.shared.presentation.PdfFailureHandler;
import io.github.lukmnh.toolbox.pdf.split.application.SplitPdfUseCase;
import io.github.lukmnh.toolbox.pdf.split.domain.SplitMode;
import io.github.lukmnh.toolbox.pdf.split.domain.SplitRequest;
import io.github.lukmnh.toolbox.pdf.split.domain.SplitResult;
import java.nio.file.Path;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** State dan aksi tool Pisah PDF. */
public final class SplitViewModel {

    private static final String SPLIT_FOLDER_SUFFIX = "_split";

    private final SplitPdfUseCase splitUseCase;
    private final InspectPdfUseCase inspectUseCase;
    private final ToolContext context;
    private final TempFiles tempFiles;

    private final ObjectProperty<PdfSource> source = new SimpleObjectProperty<>();
    private final ObjectProperty<PdfDocumentInfo> info = new SimpleObjectProperty<>();
    private final ObjectProperty<SplitChoice> mode = new SimpleObjectProperty<>(SplitChoice.EACH_PAGE);
    private final StringProperty rangeSpecification = new SimpleStringProperty("");
    private final ObjectProperty<SplitResult> result = new SimpleObjectProperty<>();
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final ObjectProperty<RunningTask> currentTask = new SimpleObjectProperty<>();
    private RunningTask runningTask;

    public SplitViewModel(
            SplitPdfUseCase splitUseCase, InspectPdfUseCase inspectUseCase, ToolContext context, TempFiles tempFiles) {
        this.splitUseCase = splitUseCase;
        this.inspectUseCase = inspectUseCase;
        this.context = context;
        this.tempFiles = tempFiles;
        mode.addListener((observable, previous, current) -> result.set(null));
        rangeSpecification.addListener((observable, previous, current) -> result.set(null));
    }

    public ReadOnlyObjectProperty<PdfDocumentInfo> infoProperty() {
        return info;
    }

    ObjectProperty<SplitChoice> modeProperty() {
        return mode;
    }

    public StringProperty rangeSpecificationProperty() {
        return rangeSpecification;
    }

    public ReadOnlyObjectProperty<SplitResult> resultProperty() {
        return result;
    }

    public BooleanProperty busyProperty() {
        return busy;
    }

    public ReadOnlyObjectProperty<RunningTask> currentTaskProperty() {
        return currentTask;
    }

    public BooleanBinding canSplit() {
        return info.isNotNull().and(busy.not());
    }

    public void openFile(Path file) {
        inspect(PdfSource.of(file));
    }

    private void inspect(PdfSource candidate) {
        track(context.tasks()
                .submit(
                        (progress, cancellation) -> inspectUseCase.execute(candidate),
                        inspected -> {
                            source.set(candidate);
                            info.set(inspected);
                            result.set(null);
                        },
                        error -> PdfFailureHandler.handle(
                                context.owner(),
                                error,
                                (file, password) -> inspect(candidate.withPassword(password)))));
    }

    public void split() {
        PdfSource current = source.get();
        SplitMode splitMode = mode.get() == SplitChoice.EACH_PAGE
                ? new SplitMode.EachPage()
                : new SplitMode.Ranges(rangeSpecification.get());
        SplitRequest request = new SplitRequest(current, splitMode, tempFiles.createDirectory(), baseName(current));
        track(context.tasks()
                .submit(
                        (progress, cancellation) -> splitUseCase.execute(request, progress, cancellation),
                        result::set,
                        error -> PdfFailureHandler.handle(context.owner(), error, (file, password) -> {
                            source.set(current.withPassword(password));
                            split();
                        })));
    }

    private void track(RunningTask task) {
        runningTask = task;
        busy.set(true);
        task.runningProperty().addListener((observable, wasRunning, running) -> busy.set(running));
        currentTask.set(task);
    }

    public void download() {
        if (result.get() != null) {
            context.downloads().downloadDirectory(result.get().outputDirectory(), outputFolderName());
        }
    }

    public void saveAs() {
        if (result.get() != null) {
            context.downloads().saveDirectoryAs(result.get().outputDirectory(), outputFolderName());
        }
    }

    private String outputFolderName() {
        return baseName(source.get()) + SPLIT_FOLDER_SUFFIX;
    }

    private static String baseName(PdfSource pdf) {
        return FileExtensions.baseName(pdf.file());
    }

    public String infoSummary(PdfDocumentInfo document) {
        return Messages.format(
                "pdf.info.summary",
                document.file().getFileName(),
                document.pageCount(),
                ByteSizeFormat.format(document.sizeBytes()));
    }

    public String resultSummary(SplitResult split) {
        return Messages.format("pdf.split.done", split.files().size());
    }

    public void dispose() {
        if (runningTask != null) {
            runningTask.cancel();
        }
        tempFiles.close();
    }
}
