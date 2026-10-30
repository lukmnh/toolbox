package io.github.lukmnh.toolbox.common.ui;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Komponen layout bersama lintas module tool (header ber-eyebrow, card, badge, segmented control, area kode, pesan
 * inline) supaya semua tool tampil konsisten. Warna dari token tema di {@code theme-light.css}, bukan hard-code.
 */
public final class ToolLayout {

    public static final double SPACING = 12;
    public static final double GAP = 8;
    private static final double PAGE_PADDING = 24;
    private static final List<String> TONE_CLASSES = List.of(
            "badge-neutral",
            "badge-info",
            "badge-success",
            "badge-warning",
            "badge-danger",
            "inline-neutral",
            "inline-info",
            "inline-success",
            "inline-warning",
            "inline-error");

    private ToolLayout() {}

    /** Root view tool: latar tema + padding standar. */
    public static VBox page() {
        VBox page = new VBox(SPACING * 1.5);
        page.getStyleClass().add("tool-view");
        page.setPadding(new Insets(PAGE_PADDING));
        return page;
    }

    /** Eyebrow = nama kategori tool. */
    public static VBox header(ToolCategory category, String title, String description) {
        Label eyebrow = new Label(Messages.get(category.messageKey()).toUpperCase(Messages.LOCALE));
        eyebrow.getStyleClass().add("tool-eyebrow");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("tool-title");
        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("tool-description");
        descriptionLabel.setWrapText(true);
        return new VBox(4, eyebrow, titleLabel, descriptionLabel);
    }

    public static VBox card(Node... content) {
        VBox card = new VBox(SPACING, content);
        card.getStyleClass().add("card");
        return card;
    }

    /** Card dengan judul; {@code trailing} (misalnya tombol Copy) rata kanan di baris judul. */
    public static VBox card(String title, List<Node> trailing, Node... content) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");
        HBox titleRow = row(titleLabel, spacer());
        titleRow.getChildren().addAll(trailing);
        VBox card = card(content);
        card.getChildren().addFirst(titleRow);
        return card;
    }

    public static Label hint(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("card-hint");
        label.setWrapText(true);
        return label;
    }

    public static Label badge(String text, Tone tone) {
        Label badge = new Label(text);
        badge.getStyleClass().add("badge");
        setTone(badge, tone);
        return badge;
    }

    /** Ganti teks + warna badge/pesan inline sekaligus. */
    public static void update(Label label, String text, Tone tone) {
        label.setText(text);
        setTone(label, tone);
    }

    public static void setTone(Label label, Tone tone) {
        label.getStyleClass().removeAll(TONE_CLASSES);
        boolean inline = label.getStyleClass().contains("inline-message");
        label.getStyleClass().add(inline ? tone.inlineClass() : tone.badgeClass());
    }

    /** Pesan inline (error/peringatan/info) yang otomatis tersembunyi saat teksnya kosong. */
    public static Label inlineMessage(Tone tone) {
        Label label = new Label();
        label.getStyleClass().add("inline-message");
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        setTone(label, tone);
        label.visibleProperty().bind(label.textProperty().isNotEmpty());
        collapseWhenHidden(label);
        return label;
    }

    /**
     * Segmented control yang terikat dua arah ke {@code value}. Selalu ada satu pilihan aktif (klik ulang tidak
     * mengosongkan pilihan).
     */
    public static <T> HBox segmented(ObjectProperty<T> value, List<T> options, Function<T, String> label) {
        ToggleGroup group = new ToggleGroup();
        HBox box = new HBox();
        box.getStyleClass().add("segmented");
        box.setAlignment(Pos.CENTER_LEFT);
        for (int index = 0; index < options.size(); index++) {
            T option = options.get(index);
            ToggleButton button = new ToggleButton(label.apply(option));
            button.setUserData(option);
            button.setToggleGroup(group);
            button.setSelected(option.equals(value.get()));
            if (index == 0) {
                button.getStyleClass().add("segment-first");
            }
            if (index == options.size() - 1) {
                button.getStyleClass().add("segment-last");
            }
            box.getChildren().add(button);
        }
        group.selectedToggleProperty().addListener((observable, previous, current) -> {
            if (current == null) {
                group.selectToggle(previous);
                return;
            }
            @SuppressWarnings("unchecked")
            T selected = (T) current.getUserData();
            value.set(selected);
        });
        value.addListener((observable, previous, current) -> group.getToggles().stream()
                .filter(toggle -> toggle.getUserData().equals(current))
                .findFirst()
                .ifPresent(group::selectToggle));
        return box;
    }

    public static TextArea codeArea(boolean editable) {
        TextArea area = new TextArea();
        area.getStyleClass().add("code-area");
        area.setEditable(editable);
        area.setWrapText(true);
        return area;
    }

    public static TextField codeField() {
        TextField field = new TextField();
        field.getStyleClass().add("code-field");
        return field;
    }

    /** Font monospace tanpa latar kode, untuk kontrol yang sudah punya gaya sendiri. */
    public static <T extends Control> T mono(T control) {
        control.getStyleClass().add("mono");
        return control;
    }

    public static Label statText() {
        Label label = new Label();
        label.getStyleClass().add("stat-text");
        return label;
    }

    /** Label kecil di atas kontrol. */
    public static VBox labeled(String label, Node control) {
        Label caption = new Label(label);
        caption.getStyleClass().add("field-label");
        return new VBox(4, caption, control);
    }

    public static HBox row(Node... children) {
        HBox row = new HBox(GAP, children);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    public static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    public static Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.setOnAction(event -> action.run());
        return button;
    }

    public static Button accentButton(String text, Runnable action) {
        Button button = button(text, action);
        button.getStyleClass().add("accent");
        return button;
    }

    /** Baris tombol Download (utama) dan Simpan sebagai…. */
    public static HBox downloadButtons(Runnable onDownload, Runnable onSaveAs) {
        return downloadButtons(
                button(Messages.get("download.button"), onDownload),
                button(Messages.get("download.saveAs.button"), onSaveAs));
    }

    /** Varian untuk tombol yang sudah dibuat pemanggil (misalnya karena state disable-nya di-bind). */
    public static HBox downloadButtons(Button download, Button saveAs) {
        download.getStyleClass().add("accent");
        HBox row = row(download, saveAs);
        row.setAlignment(Pos.CENTER_RIGHT);
        return row;
    }

    /** Area drop + tombol pilih file. */
    public static VBox dropZone(String hint, String buttonLabel, Runnable onChoose) {
        Label hintLabel = new Label(hint);
        hintLabel.getStyleClass().add("drop-hint");
        hintLabel.setWrapText(true);
        VBox zone = new VBox(SPACING, hintLabel, accentButton(buttonLabel, onChoose));
        zone.setAlignment(Pos.CENTER);
        zone.getStyleClass().add("drop-zone");
        return zone;
    }

    /**
     * @param extensions ekstensi diterima; kosong berarti semua file
     * @param onRejected menerima daftar nama file yang ditolak, dipisah koma
     */
    public static void acceptDrops(
            Node target, Set<String> extensions, Consumer<List<Path>> onAccepted, Consumer<String> onRejected) {
        if (!extensions.isEmpty()) {
            FileDropZone.install(target, extensions, onAccepted, rejected -> onRejected.accept(names(rejected)));
            return;
        }
        target.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        target.setOnDragDropped(event -> {
            boolean hasFiles = event.getDragboard().hasFiles();
            if (hasFiles) {
                onAccepted.accept(event.getDragboard().getFiles().stream()
                        .map(File::toPath)
                        .toList());
            }
            event.setDropCompleted(hasFiles);
            event.consume();
        });
    }

    public static String names(List<Path> files) {
        return files.stream().map(path -> path.getFileName().toString()).collect(Collectors.joining(", "));
    }

    /** Node hanya memakan tempat saat terlihat. */
    public static void collapseWhenHidden(Node node) {
        node.managedProperty().bind(node.visibleProperty());
    }

    /** Converter untuk ChoiceBox/ComboBox yang hanya dipilih, tidak diketik. */
    public static <T> StringConverter<T> labels(Function<T, String> label) {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : label.apply(value);
            }

            @Override
            public T fromString(String text) {
                throw new UnsupportedOperationException("Pilihan tidak bisa diketik");
            }
        };
    }
}
