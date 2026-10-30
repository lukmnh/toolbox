package io.github.lukmnh.toolbox.pdf.compress.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.FileExtensions;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.pdf.compress.application.CompressPdfUseCase;
import io.github.lukmnh.toolbox.pdf.compress.domain.CompressRequest;
import io.github.lukmnh.toolbox.pdf.compress.domain.CompressResult;
import io.github.lukmnh.toolbox.pdf.compress.domain.CompressionPreset;
import io.github.lukmnh.toolbox.pdf.shared.application.InspectPdfUseCase;
import io.github.lukmnh.toolbox.pdf.shared.domain.OutputProtection;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfDocumentInfo;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import io.github.lukmnh.toolbox.pdf.shared.presentation.PdfFailureHandler;
import java.nio.file.Path;
import java.text.NumberFormat;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

/** State dan aksi tool Kompres PDF. */
public final class CompressViewModel {

    private static final String OUTPUT_SUFFIX = "_compressed.pdf";
    private static final int PERCENT_FRACTION_DIGITS = 1;

    private final CompressPdfUseCase compressUseCase;
    private final InspectPdfUseCase inspectUseCase;
    private final ToolContext context;
    private final TempFiles tempFiles;

    private final ObjectProperty<PdfSource> source = new SimpleObjectProperty<>();
    private final ObjectProperty<PdfDocumentInfo> info = new SimpleObjectProperty<>();
    private final ObjectProperty<CompressionPreset> preset = new SimpleObjectProperty<>(CompressionPreset.DEFAULT);
    private final ObjectProperty<CompressResult> result = new SimpleObjectProperty<>();
    private final BooleanProperty removeProtection = new SimpleBooleanProperty();
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final ObjectProperty<RunningTask> currentTask = new SimpleObjectProperty<>();
    private RunningTask runningTask;

    public CompressViewModel(
            CompressPdfUseCase compressUseCase,
            InspectPdfUseCase inspectUseCase,
            ToolContext context,
            TempFiles tempFiles) {
        this.compressUseCase = compressUseCase;
        this.inspectUseCase = inspectUseCase;
        this.context = context;
        this.tempFiles = tempFiles;
        preset.addListener((observable, previous, current) -> result.set(null));
        removeProtection.addListener((observable, previous, current) -> result.set(null));
    }

    public ReadOnlyObjectProperty<PdfDocumentInfo> infoProperty() {
        return info;
    }

    public ObjectProperty<CompressionPreset> presetProperty() {
        return preset;
    }

    public BooleanProperty removeProtectionProperty() {
        return removeProtection;
    }

    public ReadOnlyObjectProperty<CompressResult> resultProperty() {
        return result;
    }

    public ReadOnlyObjectProperty<RunningTask> currentTaskProperty() {
        return currentTask;
    }

    public BooleanBinding canCompress() {
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

    public void compress() {
        PdfSource current = source.get();
        CompressRequest request = new CompressRequest(
                current,
                preset.get(),
                tempFiles.createFile(".pdf"),
                removeProtection.get() ? OutputProtection.REMOVE : OutputProtection.KEEP);
        track(context.tasks()
                .submit(
                        (progress, cancellation) -> compressUseCase.execute(request, progress, cancellation),
                        result::set,
                        error -> PdfFailureHandler.handle(context.owner(), error, (file, password) -> {
                            source.set(current.withPassword(password));
                            compress();
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
            context.downloads().download(result.get().output(), outputName());
        }
    }

    public void saveAs() {
        if (result.get() != null) {
            context.downloads().saveAs(result.get().output(), outputName());
        }
    }

    private String outputName() {
        return FileExtensions.baseName(source.get().file()) + OUTPUT_SUFFIX;
    }

    public String infoSummary(PdfDocumentInfo document) {
        return Messages.format(
                "pdf.info.summary",
                document.file().getFileName(),
                document.pageCount(),
                ByteSizeFormat.format(document.sizeBytes()));
    }

    public String resultSummary(CompressResult compressed) {
        if (compressed.alreadyOptimal()) {
            return Messages.format("pdf.compress.optimal", ByteSizeFormat.format(compressed.originalSize()));
        }
        NumberFormat percent = NumberFormat.getNumberInstance(Messages.LOCALE);
        percent.setMaximumFractionDigits(PERCENT_FRACTION_DIGITS);
        return Messages.format(
                "pdf.compress.done",
                ByteSizeFormat.format(compressed.originalSize()),
                ByteSizeFormat.format(compressed.resultSize()),
                percent.format(compressed.savingPercent()),
                compressed.imagesRecompressed());
    }

    public static String presetLabel(CompressionPreset value) {
        return switch (value) {
            case BALANCED -> Messages.get("pdf.compress.preset.balanced");
            case HIGH_QUALITY -> Messages.get("pdf.compress.preset.high");
            case SMALL_SIZE -> Messages.get("pdf.compress.preset.small");
        };
    }

    public void dispose() {
        if (runningTask != null) {
            runningTask.cancel();
        }
        tempFiles.close();
    }
}
