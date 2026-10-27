package io.github.lukmnh.toolbox.pdf.sign.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.FileExtensions;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.pdf.shared.domain.FontSpec;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import io.github.lukmnh.toolbox.pdf.shared.domain.PreviewScale;
import io.github.lukmnh.toolbox.pdf.shared.presentation.PdfErrorMessages;
import io.github.lukmnh.toolbox.pdf.shared.presentation.PdfFailureHandler;
import io.github.lukmnh.toolbox.pdf.sign.domain.EditDefaults;
import io.github.lukmnh.toolbox.pdf.sign.domain.EditSession;
import io.github.lukmnh.toolbox.pdf.sign.domain.FieldValue;
import io.github.lukmnh.toolbox.pdf.sign.domain.FormField;
import io.github.lukmnh.toolbox.pdf.sign.domain.Overlay;
import io.github.lukmnh.toolbox.pdf.sign.domain.SignatureStroke;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.text.Font;

/** State dan aksi tool Isi & TTD. Semua method dipanggil dari FX thread. */
public final class SignViewModel {

    private static final String OUTPUT_SUFFIX = "_edited.pdf";
    private static final double FONT_PROBE_SIZE = 12;

    private final SignUseCases useCases;
    private final ToolContext context;
    private final TempFiles tempFiles;

    private final ObjectProperty<PdfSource> source = new SimpleObjectProperty<>();
    private final IntegerProperty pageIndex = new SimpleIntegerProperty();
    private final IntegerProperty pageCount = new SimpleIntegerProperty();
    private final ObjectProperty<Image> pageImage = new SimpleObjectProperty<>();
    private final ObjectProperty<EditMode> mode = new SimpleObjectProperty<>(EditMode.SELECT);
    private final DoubleProperty fontSize = new SimpleDoubleProperty(EditDefaults.DEFAULT_FONT_SIZE);
    private final ObjectProperty<BufferedImage> signature = new SimpleObjectProperty<>();
    private final ObjectProperty<Image> signaturePreview = new SimpleObjectProperty<>();
    private final ObservableList<OverlayItem> overlays = FXCollections.observableArrayList();
    private final ObservableList<FormField> formFields = FXCollections.observableArrayList();
    private final ObjectProperty<OverlayItem> selected = new SimpleObjectProperty<>();
    private final BooleanProperty dirty = new SimpleBooleanProperty();
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final StringProperty fontFamily =
            new SimpleStringProperty(Font.getDefault().getFamily());
    private final ObjectProperty<RunningTask> currentTask = new SimpleObjectProperty<>();

    private final Set<RunningTask> activeTasks = new HashSet<>();
    private EditSession session;
    private long renderGeneration;
    private boolean disposed;

    public SignViewModel(SignUseCases useCases, ToolContext context, TempFiles tempFiles) {
        this.useCases = useCases;
        this.context = context;
        this.tempFiles = tempFiles;
        fontSize.addListener((observable, previous, current) -> applyFontSizeToSelection(current.doubleValue()));
    }

    // ---------- Properti untuk binding ----------

    ReadOnlyObjectProperty<PdfSource> sourceProperty() {
        return source;
    }

    ReadOnlyIntegerProperty pageIndexProperty() {
        return pageIndex;
    }

    ReadOnlyIntegerProperty pageCountProperty() {
        return pageCount;
    }

    ReadOnlyObjectProperty<Image> pageImageProperty() {
        return pageImage;
    }

    ObjectProperty<EditMode> modeProperty() {
        return mode;
    }

    DoubleProperty fontSizeProperty() {
        return fontSize;
    }

    ReadOnlyObjectProperty<Image> signaturePreviewProperty() {
        return signaturePreview;
    }

    ObservableList<OverlayItem> overlays() {
        return overlays;
    }

    ObservableList<FormField> formFields() {
        return formFields;
    }

    ObjectProperty<OverlayItem> selectedProperty() {
        return selected;
    }

    ReadOnlyBooleanProperty dirtyProperty() {
        return dirty;
    }

    ReadOnlyBooleanProperty busyProperty() {
        return busy;
    }

    ReadOnlyStringProperty fontFamilyProperty() {
        return fontFamily;
    }

    ReadOnlyObjectProperty<RunningTask> currentTaskProperty() {
        return currentTask;
    }

    public boolean isDirty() {
        return dirty.get();
    }

    // ---------- Dokumen ----------

    public void openFile(Path file) {
        open(PdfSource.of(file));
    }

    private void open(PdfSource candidate) {
        submit(
                () -> {
                    EditSession opened = useCases.open().execute(candidate);
                    return new OpenedDocument(opened, opened.formFields(), opened.fontSpec());
                },
                opened -> showDocument(candidate, opened),
                error -> PdfFailureHandler.handle(
                        context.owner(), error, (file, password) -> open(candidate.withPassword(password))));
    }

    private record OpenedDocument(EditSession session, List<FormField> fields, FontSpec fontSpec) {}

    private void showDocument(PdfSource opened, OpenedDocument document) {
        if (disposed) {
            document.session().close();
            return;
        }
        closeSession();
        session = document.session();
        source.set(opened);
        fontFamily.set(previewFontFamily(document.fontSpec()));
        overlays.clear();
        selected.set(null);
        formFields.setAll(document.fields());
        pageCount.set(session.pageCount());
        pageIndex.set(0);
        dirty.set(false);
        renderCurrentPage();
    }

    /** Muat file TTF yang sama dengan PDF supaya lebar dan posisi teks identik. */
    private static String previewFontFamily(FontSpec spec) {
        return switch (spec) {
            case FontSpec.FileFont fileFont -> {
                Font loaded = Font.loadFont(fileFont.file().toUri().toString(), FONT_PROBE_SIZE);
                yield loaded == null ? fileFont.family() : loaded.getFamily();
            }
            case FontSpec.BuiltinHelvetica helvetica -> helvetica.family();
        };
    }

    public void goToPage(int index) {
        if (session == null || index < 0 || index >= pageCount.get() || index == pageIndex.get()) {
            return;
        }
        finishSelection();
        pageIndex.set(index);
        renderCurrentPage();
    }

    private void renderCurrentPage() {
        EditSession current = session;
        int index = pageIndex.get();
        long generation = ++renderGeneration;
        submit(
                () -> SwingFXUtils.toFXImage(useCases.render().execute(current, index), null),
                image -> {
                    // Abaikan hasil render lama bila user sudah pindah halaman lagi
                    if (generation == renderGeneration) {
                        pageImage.set(image);
                    }
                },
                this::showError);
    }

    // ---------- Overlay ----------

    OverlayItem.TextItem addText(double x, double baselineY) {
        OverlayItem.TextItem item = new OverlayItem.TextItem(pageIndex.get(), x, baselineY, fontSize.get());
        overlays.add(item);
        selected.set(item);
        dirty.set(true);
        return item;
    }

    /** @return kosong bila belum ada tanda tangan; view lalu meminta user membuatnya */
    Optional<OverlayItem.SignatureItem> addSignature(double centerX, double centerY) {
        BufferedImage image = signature.get();
        if (image == null) {
            return Optional.empty();
        }
        double width = PreviewScale.toPixels(EditDefaults.DEFAULT_SIGNATURE_WIDTH);
        OverlayItem.SignatureItem item =
                new OverlayItem.SignatureItem(pageIndex.get(), 0, 0, width, image, signaturePreview.get());
        item.moveTo(centerX - width / 2, centerY - item.heightProperty().get() / 2);
        overlays.add(item);
        selected.set(item);
        dirty.set(true);
        return Optional.of(item);
    }

    void moveItem(OverlayItem item, double x, double y) {
        item.moveTo(x, y);
        dirty.set(true);
    }

    void removeItem(OverlayItem item) {
        overlays.remove(item);
        if (selected.get() == item) {
            selected.set(null);
        }
        dirty.set(true);
    }

    void appendText(OverlayItem.TextItem item, String typed) {
        item.textProperty().set(item.textProperty().get() + typed);
        dirty.set(true);
    }

    void deleteLastCharacter(OverlayItem.TextItem item) {
        String text = item.textProperty().get();
        if (!text.isEmpty()) {
            item.textProperty().set(text.substring(0, text.offsetByCodePoints(text.length(), -1)));
            dirty.set(true);
        }
    }

    /** Teks kosong otomatis dihapus saat selesai diedit. */
    void finishEditing(OverlayItem.TextItem item) {
        if (item.textProperty().get().isBlank()) {
            removeItem(item);
        }
    }

    /** Resize TTD terpilih; {@code factor} > 1 memperbesar. */
    boolean resizeSelectedSignature(double factor) {
        if (!(selected.get() instanceof OverlayItem.SignatureItem item)) {
            return false;
        }
        double minimum = PreviewScale.toPixels(EditDefaults.MIN_SIGNATURE_WIDTH);
        item.resize(Math.max(minimum, item.widthProperty().get() * factor));
        dirty.set(true);
        return true;
    }

    private void applyFontSizeToSelection(double size) {
        if (selected.get() instanceof OverlayItem.TextItem item
                && item.fontSizeProperty().get() != size) {
            item.fontSizeProperty().set(size);
            dirty.set(true);
        }
    }

    void select(OverlayItem item) {
        selected.set(item);
        if (item instanceof OverlayItem.TextItem text) {
            fontSize.set(text.fontSizeProperty().get());
        }
    }

    private void finishSelection() {
        if (selected.get() instanceof OverlayItem.TextItem text) {
            finishEditing(text);
        }
        selected.set(null);
    }

    // ---------- Form ----------

    void updateField(String fieldName, FieldValue value) {
        EditSession current = session;
        submit(
                () -> {
                    useCases.updateField().execute(current, fieldName, value);
                    return Boolean.TRUE;
                },
                done -> {
                    dirty.set(true);
                    renderCurrentPage();
                },
                this::showError);
    }

    // ---------- Tanda tangan ----------

    public void loadLastSignature() {
        submit(
                () -> useCases.loadLastSignature().execute(),
                loaded -> loaded.ifPresent(this::setSignature),
                this::showError);
    }

    void saveDrawnSignature(List<SignatureStroke> strokes) {
        submit(
                () -> useCases.saveDrawnSignature().execute(strokes),
                drawn -> drawn.ifPresent(this::setSignature),
                this::showError);
    }

    void importSignature(Path imageFile) {
        submit(() -> useCases.importSignature().execute(imageFile), this::setSignature, this::showError);
    }

    private void setSignature(BufferedImage image) {
        signature.set(image);
        signaturePreview.set(SwingFXUtils.toFXImage(image, null));
    }

    boolean hasSignature() {
        return signature.get() != null;
    }

    // ---------- Download ----------

    void download() {
        export(output -> context.downloads().download(output, outputName()));
    }

    void saveAs() {
        export(output -> context.downloads().saveAs(output, outputName()));
    }

    private void export(Consumer<Path> deliver) {
        if (session == null) {
            return;
        }
        finishSelection();
        EditSession current = session;
        List<Overlay> snapshot = overlays.stream().map(OverlayItem::toOverlay).toList();
        Path output = tempFiles.createFile(".pdf");
        submitCancellable(
                cancellation -> useCases.export().execute(current, snapshot, output, cancellation),
                exported -> {
                    dirty.set(false);
                    deliver.accept(exported);
                },
                this::showError);
    }

    private String outputName() {
        return FileExtensions.baseName(source.get().file()) + OUTPUT_SUFFIX;
    }

    String pageLabel() {
        return Messages.format("pdf.sign.page.of", pageCount.get());
    }

    // ---------- Infrastruktur ----------

    private <T> void submit(Work<T> work, Consumer<? super T> onSuccess, Consumer<? super Throwable> onFailure) {
        submitCancellable(cancellation -> work.run(), onSuccess, onFailure);
    }

    private <T> void submitCancellable(
            CancellableWork<T> work, Consumer<? super T> onSuccess, Consumer<? super Throwable> onFailure) {
        RunningTask task = context.tasks()
                .submit(
                        (progress, cancellation) -> work.run(cancellation),
                        value -> {
                            if (!disposed) {
                                onSuccess.accept(value);
                            }
                        },
                        onFailure);
        activeTasks.add(task);
        busy.set(true);
        task.runningProperty().addListener((observable, wasRunning, running) -> {
            if (!running) {
                activeTasks.remove(task);
                busy.set(!activeTasks.isEmpty());
            }
        });
        currentTask.set(task);
    }

    @FunctionalInterface
    private interface Work<T> {
        T run() throws Exception;
    }

    @FunctionalInterface
    private interface CancellableWork<T> {
        T run(CancellationToken cancellation) throws Exception;
    }

    private void showError(Throwable error) {
        if (!disposed) {
            PdfErrorMessages.show(context.owner(), error);
        }
    }

    void warn(String message) {
        context.notifier().warning(message);
    }

    boolean confirmDiscard() {
        return !dirty.get()
                || ErrorDialogs.confirm(
                        context.owner(),
                        Messages.get("confirm.discard.header"),
                        Messages.get("confirm.discard.detail"),
                        Messages.get("confirm.discard.button"));
    }

    private void closeSession() {
        if (session != null) {
            session.close();
            session = null;
        }
    }

    public void dispose() {
        disposed = true;
        List.copyOf(activeTasks).forEach(RunningTask::cancel);
        closeSession();
        overlays.clear();
        formFields.clear();
        pageImage.set(null);
        tempFiles.close();
    }
}
