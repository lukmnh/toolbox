package io.github.lukmnh.toolbox.devutils.diff.presentation;

import io.github.lukmnh.toolbox.devutils.diff.domain.DiffRow;
import io.github.lukmnh.toolbox.devutils.diff.domain.RowType;
import io.github.lukmnh.toolbox.devutils.diff.domain.Segment;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

/**
 * Satu baris side-by-side: [nomor | teks asli] [nomor | teks baru]. Kedua sisi ada di cell yang sama, jadi scroll kiri
 * dan kanan otomatis sinkron. Teks panjang dipotong (clip), tidak di-wrap, supaya tinggi cell tetap dan
 * {@code ListView} virtual tetap cepat untuk 100 ribu baris.
 */
final class SideBySideCell extends ListCell<DiffRow> {

    private static final String TAB_AS_SPACES = "    ";
    private static final List<String> SIDE_CLASSES =
            List.of("diff-side-insert", "diff-side-delete", "diff-side-change", "diff-side-empty");

    private final Label oldNumber = gutter();
    private final Label newNumber = gutter();
    private final HBox oldText = textBox();
    private final HBox newText = textBox();
    private final HBox oldSide = side(oldNumber, oldText);
    private final HBox newSide = side(newNumber, newText);
    private final HBox row = new HBox(oldSide, newSide);

    SideBySideCell() {
        getStyleClass().add("diff-cell");
    }

    @Override
    protected void updateItem(DiffRow item, boolean empty) {
        super.updateItem(item, empty);
        setText(null);
        if (empty || item == null) {
            setGraphic(null);
            return;
        }
        fill(
                oldSide,
                oldNumber,
                oldText,
                item.oldNumber(),
                item.oldSegments(),
                oldSideClass(item.type()),
                "diff-word-delete",
                item);
        fill(
                newSide,
                newNumber,
                newText,
                item.newNumber(),
                item.newSegments(),
                newSideClass(item.type()),
                "diff-word-insert",
                item);
        setGraphic(row);
    }

    private static void fill(
            HBox side,
            Label number,
            HBox text,
            int lineNumber,
            List<Segment> segments,
            String sideClass,
            String wordClass,
            DiffRow item) {
        side.getStyleClass().removeAll(SIDE_CLASSES);
        if (!sideClass.isEmpty()) {
            side.getStyleClass().add(sideClass);
        }
        number.setText(lineNumber == DiffRow.NO_LINE ? "" : Integer.toString(lineNumber));
        text.getChildren().clear();
        // Highlight per kata hanya bermakna pada baris yang dipasangkan; baris tambah/hapus sudah berwarna penuh
        boolean highlightWords = item.type() == RowType.CHANGE;
        for (Segment segment : segments) {
            Label label = new Label(segment.text().replace("\t", TAB_AS_SPACES));
            label.getStyleClass().add("diff-text");
            label.setMinWidth(Region.USE_PREF_SIZE);
            if (highlightWords && segment.changed()) {
                label.getStyleClass().add(wordClass);
            }
            text.getChildren().add(label);
        }
    }

    private static String oldSideClass(RowType type) {
        return switch (type) {
            case EQUAL -> "";
            case DELETE -> "diff-side-delete";
            case INSERT -> "diff-side-empty";
            case CHANGE -> "diff-side-change";
        };
    }

    private static String newSideClass(RowType type) {
        return switch (type) {
            case EQUAL -> "";
            case DELETE -> "diff-side-empty";
            case INSERT -> "diff-side-insert";
            case CHANGE -> "diff-side-change";
        };
    }

    private static Label gutter() {
        Label label = new Label();
        label.getStyleClass().add("diff-gutter");
        label.setMaxHeight(Double.MAX_VALUE);
        return label;
    }

    /** Kotak teks dengan clip supaya baris panjang tidak menimpa sisi sebelahnya. */
    private static HBox textBox() {
        HBox box = new HBox();
        box.getStyleClass().add("diff-text-box");
        box.setMinWidth(0);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(box.widthProperty());
        clip.heightProperty().bind(box.heightProperty());
        box.setClip(clip);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    /** Pref width 0 + grow: kedua sisi selalu mendapat lebar yang sama. */
    private static HBox side(Label number, HBox text) {
        HBox side = new HBox(number, text);
        side.setMinWidth(0);
        side.setPrefWidth(0);
        HBox.setHgrow(side, Priority.ALWAYS);
        return side;
    }
}
