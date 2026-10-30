package io.github.lukmnh.toolbox.app.guide;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.AtomicFiles;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.settings.Theme;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.Debounce;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;

/**
 * Guide di Home: editor Markdown di kiri, preview live di kanan. Perubahan disimpan otomatis (debounce) ke
 * {@code ~/.toolbox/guide.md}; file kecil, jadi ditulis langsung di FX thread supaya urutan simpan pasti.
 *
 * <p>Wajib {@link #dispose()} saat view dibuang: simpan perubahan yang tertunda dan lepas listener tema.
 */
public final class GuideView extends VBox {

    private static final System.Logger LOG = System.getLogger(GuideView.class.getName());
    private static final Duration DEBOUNCE = Duration.millis(300);
    private static final String FILE_BASE_NAME = "toolbox-guide";

    private final ToolContext context;
    private final ObservableValue<Theme> theme;
    private final GuideStore store;
    private final GuideHtml html = new GuideHtml();
    private final GuidePdf pdf = new GuidePdf();
    private final TextArea editor = ToolLayout.codeArea(true);
    private final WebView preview = new WebView();
    private final Label message = ToolLayout.inlineMessage(Tone.DANGER);
    private final Debounce debounce = new Debounce(DEBOUNCE, this::renderAndSave);
    private final ChangeListener<Theme> themeListener = (observable, previous, current) -> render();
    private boolean dirty;
    private String renderedHtml = "";

    public GuideView(ToolContext context, ObservableValue<Theme> theme, GuideStore store) {
        super(ToolLayout.SPACING);
        this.context = context;
        this.theme = theme;
        this.store = store;
        getStyleClass().add("tool-view");
        setPadding(new Insets(0, 32, 32, 32));

        editor.getStyleClass().add("guide-editor");
        editor.setText(loadGuide());
        editor.textProperty().addListener((observable, previous, current) -> {
            dirty = true;
            debounce.trigger();
        });

        preview.getStyleClass().add("guide-preview");
        preview.getEngine().setJavaScriptEnabled(false);
        preview.setContextMenuEnabled(false);
        openLinksExternally(preview.getEngine());

        SplitPane split = new SplitPane(
                ToolLayout.labeled(Messages.get("app.guide.editor"), editor),
                ToolLayout.labeled(Messages.get("app.guide.preview"), preview));
        VBox.setVgrow(editor, Priority.ALWAYS);
        VBox.setVgrow(preview, Priority.ALWAYS);
        split.setDividerPositions(0.5);
        VBox.setVgrow(split, Priority.ALWAYS);

        getChildren().addAll(toolbar(), message, split);
        theme.addListener(themeListener);
        render();
    }

    private HBox toolbar() {
        Label hint = ToolLayout.hint(Messages.get("app.guide.hint"));
        HBox.setHgrow(hint, Priority.ALWAYS);
        hint.setMaxWidth(Double.MAX_VALUE);
        Button reset = ToolLayout.button(Messages.get("app.guide.reset"), this::confirmReset);
        Button markdown = ToolLayout.button(Messages.get("app.guide.downloadMd"), this::downloadMarkdown);
        Button pdfButton = ToolLayout.accentButton(Messages.get("app.guide.downloadPdf"), this::downloadPdf);
        return ToolLayout.row(hint, reset, markdown, pdfButton);
    }

    private String loadGuide() {
        try {
            return store.load();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Gagal membaca guide user, memakai guide bawaan", e);
            message.setText(Messages.format("app.guide.error.load", ErrorDialogs.describe(e)));
            return GuideStore.defaultGuide();
        }
    }

    private void renderAndSave() {
        render();
        save();
    }

    private void render() {
        renderedHtml = html.document(editor.getText(), theme.getValue());
        preview.getEngine().loadContent(renderedHtml);
    }

    private void save() {
        if (!dirty) {
            return;
        }
        try {
            store.save(editor.getText());
            dirty = false;
            message.setText("");
        } catch (IOException | UncheckedIOException e) {
            LOG.log(Level.WARNING, "Gagal menyimpan guide", e);
            message.setText(Messages.format("app.guide.error.save", ErrorDialogs.describe(e)));
        }
    }

    private void confirmReset() {
        boolean confirmed = ErrorDialogs.confirm(
                context.owner(),
                Messages.get("app.guide.reset.header"),
                Messages.get("app.guide.reset.detail"),
                Messages.get("app.guide.reset"));
        if (!confirmed) {
            return;
        }
        try {
            String defaultGuide = store.reset();
            editor.setText(defaultGuide);
            // setText memicu listener edit; guide bawaan tidak perlu ditulis ulang ke file user
            debounce.stop();
            dirty = false;
            message.setText("");
            render();
        } catch (IOException | UncheckedIOException e) {
            ErrorDialogs.show(context.owner(), Messages.get("app.guide.error.reset"), ErrorDialogs.describe(e));
        }
    }

    private void downloadMarkdown() {
        String markdown = editor.getText();
        export("md", file -> Files.writeString(file, markdown, StandardCharsets.UTF_8));
    }

    private void downloadPdf() {
        String markdown = editor.getText();
        String fallbackTitle = Messages.get("app.guide.pdfTitle");
        export("pdf", file -> pdf.write(markdown, fallbackTitle, file));
    }

    /** Tulis ke temp file di background, lalu serahkan ke DownloadService (temp dibersihkan saat exit). */
    private void export(String extension, AtomicFiles.FileWriter writer) {
        context.tasks()
                .submit(
                        (progress, cancellation) -> {
                            Path file = Files.createTempFile(
                                    TempFiles.applicationDirectory(), FILE_BASE_NAME + "-", "." + extension);
                            writer.writeTo(file);
                            return file;
                        },
                        file -> context.downloads().download(file, FILE_BASE_NAME + "." + extension),
                        error -> ErrorDialogs.show(
                                context.owner(),
                                Messages.format("app.guide.error.export", extension.toUpperCase(Locale.ROOT)),
                                ErrorDialogs.describe(error)));
    }

    /**
     * JavaScript mati, tapi klik link tetap menavigasi WebView. Link http(s)/mailto dibuka di browser sistem lalu
     * preview dikembalikan ke isi guide.
     */
    private void openLinksExternally(WebEngine engine) {
        engine.locationProperty().addListener((observable, previous, location) -> {
            if (location == null || location.isBlank() || location.startsWith("about:")) {
                return;
            }
            String lower = location.toLowerCase(Locale.ROOT);
            if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("mailto:")) {
                context.hostServices().showDocument(location);
            }
            Platform.runLater(() -> engine.loadContent(renderedHtml));
        });
    }

    /** Simpan perubahan yang masih tertunda debounce dan lepas listener dari properti tema yang berumur panjang. */
    public void dispose() {
        debounce.stop();
        save();
        theme.removeListener(themeListener);
    }
}
