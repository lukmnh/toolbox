package io.github.lukmnh.toolbox.devutils.regex.presentation;

import java.util.List;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Teks monospace dengan latar berwarna per rentang. Latar digambar dari {@link TextFlow#rangeShape} di layer
 * terpisah di belakang teks, sehingga satu node {@link Text} cukup untuk ratusan KB teks dan ribuan highlight
 * (tidak perlu memecah teks menjadi ribuan node).
 */
final class HighlightedText extends Pane {

    private final Text text = new Text();
    private final TextFlow flow = new TextFlow(text);
    private final Group highlights = new Group();
    private List<HighlightRange> ranges = List.of();

    HighlightedText(String styleClass) {
        getStyleClass().add(styleClass);
        text.getStyleClass().add("regex-text");
        highlights.setManaged(false);
        getChildren().addAll(highlights, flow);
    }

    void setContent(String content, List<HighlightRange> newRanges) {
        text.setText(content);
        ranges = List.copyOf(newRanges);
        requestLayout();
    }

    /**
     * Highlight digambar di sini, setelah TextFlow selesai di-layout dengan font CSS yang sudah diterapkan.
     * {@code rangeShape} yang dipanggil sebelum itu memakai geometri lama sehingga latar bergeser dari teksnya.
     */
    @Override
    protected void layoutChildren() {
        double width = getWidth() - snappedLeftInset() - snappedRightInset();
        flow.resizeRelocate(snappedLeftInset(), snappedTopInset(), width, flow.prefHeight(width));
        flow.layout();
        highlights.relocate(snappedLeftInset(), snappedTopInset());
        redraw();
    }

    /** Lebar -1 berarti "tanpa batas" bagi parent; jangan dikurangi inset supaya TextFlow tidak wrap per karakter. */
    @Override
    protected double computePrefHeight(double width) {
        double inner = width < 0 ? -1 : Math.max(0, width - snappedLeftInset() - snappedRightInset());
        return snappedTopInset() + flow.prefHeight(inner) + snappedBottomInset();
    }

    private void redraw() {
        int length = text.getText() == null ? 0 : text.getText().length();
        highlights.getChildren().clear();
        for (HighlightRange range : ranges) {
            int end = Math.min(range.end(), length);
            if (range.start() >= end) {
                continue;
            }
            PathElement[] shape = flow.rangeShape(range.start(), end);
            Path path = new Path(shape);
            path.getStyleClass().addAll("regex-highlight", range.styleClass());
            path.setManaged(false);
            highlights.getChildren().add(path);
        }
    }
}
