package io.github.lukmnh.toolbox.json.csv.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.json.csv.domain.MismatchPolicy;
import io.github.lukmnh.toolbox.json.csv.domain.MismatchReport;
import io.github.lukmnh.toolbox.json.csv.domain.RowMismatch;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

/** Ringkasan baris dengan jumlah kolom tidak konsisten + daftar nomor barisnya (maks 100). */
final class MismatchPane extends VBox {

    private static final double LIST_HEIGHT = 120;

    private final String summaryKey;
    private final Label summary = ToolLayout.inlineMessage(Tone.WARNING);
    private final ListView<String> lines = new ListView<>();
    private final TitledPane details = new TitledPane(Messages.get("json.csv.mismatch.details"), lines);

    /** @param summaryKey key ringkasan: {0}=jumlah, {1}=kolom acuan, {2}=perlakuan */
    MismatchPane(String summaryKey) {
        super(ToolLayout.GAP);
        this.summaryKey = summaryKey;
        lines.setPrefHeight(LIST_HEIGHT);
        details.setExpanded(false);
        getChildren().addAll(summary, details);
        ToolLayout.collapseWhenHidden(this);
        setVisible(false);
    }

    void show(MismatchReport report) {
        if (report == null || report.isEmpty()) {
            summary.setText("");
            lines.getItems().clear();
            setVisible(false);
            return;
        }
        String treatment = Messages.get(
                report.policy() == MismatchPolicy.SKIP ? "json.csv.mismatch.skipped" : "json.csv.mismatch.padded");
        summary.setText(Messages.format(summaryKey, report.total(), report.expectedColumns(), treatment));
        lines.getItems()
                .setAll(report.entries().stream().map(MismatchPane::line).toList());
        if (report.isTruncated()) {
            lines.getItems()
                    .add(Messages.format(
                            "json.csv.mismatch.more",
                            report.total() - report.entries().size()));
        }
        setVisible(true);
    }

    private static String line(RowMismatch mismatch) {
        return Messages.format("json.csv.mismatch.line", mismatch.lineNumber(), mismatch.fieldCount());
    }
}
