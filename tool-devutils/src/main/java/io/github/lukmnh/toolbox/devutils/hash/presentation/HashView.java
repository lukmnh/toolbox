package io.github.lukmnh.toolbox.devutils.hash.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.ui.ClipboardActions;
import io.github.lukmnh.toolbox.common.ui.Notifier;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.devutils.hash.domain.DigestEncoding;
import io.github.lukmnh.toolbox.devutils.hash.domain.DigestResult;
import java.util.List;
import java.util.Map;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Layout Hash Generator: input + opsi di atas, daftar hasil per algoritma di bawah. */
public final class HashView extends VBox {

    private static final int INPUT_ROWS = 6;
    private static final double NAME_COLUMN_WIDTH = 150;

    private static final Map<DigestEncoding, String> ENCODING_LABELS = Map.of(
            DigestEncoding.HEX_LOWER, "devutils.hash.encoding.hexLower",
            DigestEncoding.HEX_UPPER, "devutils.hash.encoding.hexUpper",
            DigestEncoding.BASE64, "devutils.hash.encoding.base64");
    private static final Map<HashMode, String> MODE_LABELS = Map.of(
            HashMode.PLAIN, "devutils.hash.mode.plain",
            HashMode.HMAC, "devutils.hash.mode.hmac");

    private final HashViewModel viewModel;
    private final Notifier notifier;
    private final VBox rows = new VBox(2);

    public HashView(HashViewModel viewModel, Notifier notifier) {
        this.viewModel = viewModel;
        this.notifier = notifier;
        VBox page = ToolLayout.page();
        VBox.setVgrow(page, Priority.ALWAYS);
        ScrollPane results = new ScrollPane(buildResultsCard());
        results.setFitToWidth(true);
        results.getStyleClass().add("edge-to-edge");
        VBox.setVgrow(results, Priority.ALWAYS);
        page.getChildren()
                .addAll(
                        ToolLayout.header(
                                ToolCategory.DEV_UTILS,
                                Messages.get("devutils.hash.name"),
                                Messages.get("devutils.hash.hint")),
                        buildInputCard(),
                        buildHmacCard(),
                        results);
        getStyleClass().add("tool-view");
        getChildren().add(page);
        viewModel.outcomeProperty().addListener((observable, previous, current) -> renderRows());
        viewModel.encodingProperty().addListener((observable, previous, current) -> renderRows());
    }

    private VBox buildInputCard() {
        TextArea input = ToolLayout.codeArea(true);
        input.setPromptText(Messages.get("devutils.hash.input.prompt"));
        input.setPrefRowCount(INPUT_ROWS);
        input.textProperty().bindBidirectional(viewModel.textProperty());

        Label stats = ToolLayout.statText();
        stats.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> viewModel.outcomeProperty().get() == null
                                ? ""
                                : viewModel.statsText(
                                        viewModel.outcomeProperty().get()),
                        viewModel.outcomeProperty()));
        HBox options = ToolLayout.row(
                stats,
                ToolLayout.spacer(),
                caption("devutils.hash.mode"),
                ToolLayout.segmented(
                        viewModel.modeProperty(),
                        List.of(HashMode.values()),
                        mode -> Messages.get(MODE_LABELS.get(mode))),
                caption("devutils.hash.format"),
                ToolLayout.segmented(
                        viewModel.encodingProperty(),
                        List.of(DigestEncoding.values()),
                        encoding -> Messages.get(ENCODING_LABELS.get(encoding))));
        return ToolLayout.card(Messages.get("devutils.hash.input"), List.of(), input, options);
    }

    private VBox buildHmacCard() {
        PasswordField key = new PasswordField();
        key.setPromptText(Messages.get("devutils.hash.hmac.prompt"));
        HBox.setHgrow(key, Priority.ALWAYS);
        Button compute = ToolLayout.accentButton(Messages.get("devutils.hash.hmac.compute"), () -> {
            char[] secret = toChars(key.getCharacters());
            // Field UI dikosongkan segera supaya key tidak tertinggal di node (Security Requirements #7)
            key.clear();
            viewModel.computeHmac(secret);
        });
        compute.disableProperty().bind(key.lengthProperty().isEqualTo(0));
        key.setOnAction(event -> compute.fire());
        VBox card = ToolLayout.card(
                Messages.get("devutils.hash.hmac.title"),
                List.of(),
                ToolLayout.row(key, compute),
                ToolLayout.hint(Messages.get("devutils.hash.hmac.hint")));
        card.visibleProperty().bind(viewModel.modeProperty().isEqualTo(HashMode.HMAC));
        ToolLayout.collapseWhenHidden(card);
        return card;
    }

    private VBox buildResultsCard() {
        Label kind = ToolLayout.badge(Messages.get("devutils.hash.mode.hmac"), Tone.INFO);
        kind.visibleProperty()
                .bind(Bindings.createBooleanBinding(
                        () -> viewModel.outcomeProperty().get() != null
                                && viewModel.outcomeProperty().get().hmac(),
                        viewModel.outcomeProperty()));
        Label empty = ToolLayout.inlineMessage(Tone.NEUTRAL);
        empty.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> viewModel.outcomeProperty().get() != null
                                ? ""
                                : Messages.get(
                                        viewModel.modeProperty().get() == HashMode.HMAC
                                                ? "devutils.hash.empty.hmac"
                                                : "devutils.hash.empty"),
                        viewModel.outcomeProperty(),
                        viewModel.modeProperty()));
        return ToolLayout.card(
                Messages.get("devutils.hash.results"),
                List.of(kind, ClipboardActions.clearButton(notifier)),
                empty,
                rows);
    }

    private void renderRows() {
        HashViewModel.Outcome current = viewModel.outcomeProperty().get();
        if (current == null) {
            rows.getChildren().clear();
            return;
        }
        DigestEncoding encoding = viewModel.encodingProperty().get();
        rows.getChildren()
                .setAll(current.results().stream()
                        .map(result -> resultRow(result, encoding))
                        .toList());
    }

    private Node resultRow(DigestResult result, DigestEncoding encoding) {
        Label name = new Label(result.algorithm());
        name.getStyleClass().add("result-label");
        VBox nameColumn = new VBox(4, name);
        nameColumn.setMinWidth(NAME_COLUMN_WIDTH);
        nameColumn.setPrefWidth(NAME_COLUMN_WIDTH);
        if (result.insecure()) {
            Label badge = ToolLayout.badge(Messages.get("devutils.hash.insecure"), Tone.WARNING);
            badge.setTooltip(new Tooltip(Messages.get("devutils.hash.insecure.tooltip")));
            nameColumn.getChildren().add(badge);
        }
        Label value = new Label(result.encoded(encoding));
        value.getStyleClass().add("result-value");
        value.setWrapText(true);
        value.setMinWidth(0);
        value.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(value, Priority.ALWAYS);
        HBox row = ToolLayout.row(nameColumn, value, ClipboardActions.copyButton(value::getText, notifier));
        row.getStyleClass().add("result-row");
        return row;
    }

    private static Label caption(String key) {
        Label label = new Label(Messages.get(key));
        label.getStyleClass().add("field-label");
        return label;
    }

    /** Disalin per karakter supaya tidak membuat {@link String} yang tidak bisa di-wipe. */
    private static char[] toChars(CharSequence typed) {
        char[] chars = new char[typed.length()];
        for (int index = 0; index < chars.length; index++) {
            chars[index] = typed.charAt(index);
        }
        return chars;
    }
}
