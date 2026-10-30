package io.github.lukmnh.toolbox.devutils.hash.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ClipboardActions;
import io.github.lukmnh.toolbox.common.ui.Notifier;
import io.github.lukmnh.toolbox.common.ui.TaskStatusBar;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.devutils.hash.application.GenerateSecretUseCase;
import io.github.lukmnh.toolbox.devutils.hash.application.GenerateUuidsUseCase;
import io.github.lukmnh.toolbox.devutils.hash.domain.KeyPairAlgorithm;
import io.github.lukmnh.toolbox.devutils.hash.domain.SecretEncoding;
import io.github.lukmnh.toolbox.devutils.hash.domain.UuidVersion;
import io.github.lukmnh.toolbox.devutils.hash.presentation.GenerateViewModel.KeyPart;
import java.util.List;
import java.util.Map;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** Tab Generate: tiga card (random secret, key pair PEM, UUID) yang langsung terisi saat tab dibuka. */
final class GeneratePane extends VBox {

    private static final int KEY_ROWS = 8;
    private static final int UUID_ROWS = 6;
    private static final double SPINNER_WIDTH = 90;

    private static final Map<SecretEncoding, String> ENCODING_LABELS = Map.of(
            SecretEncoding.HEX, "devutils.hash.generate.secret.encoding.hex",
            SecretEncoding.BASE64, "devutils.hash.generate.secret.encoding.base64",
            SecretEncoding.BASE64_URL, "devutils.hash.generate.secret.encoding.base64Url");
    private static final Map<UuidVersion, String> UUID_LABELS = Map.of(
            UuidVersion.V4, "devutils.hash.generate.uuid.v4",
            UuidVersion.V7, "devutils.hash.generate.uuid.v7");
    private static final Map<KeyPart, String> KEY_TITLES = Map.of(
            KeyPart.PUBLIC, "devutils.hash.generate.keyPair.public",
            KeyPart.PRIVATE, "devutils.hash.generate.keyPair.private");

    private final GenerateViewModel viewModel;
    private final Notifier notifier;

    GeneratePane(GenerateViewModel viewModel, Notifier notifier) {
        super(ToolLayout.SPACING);
        this.viewModel = viewModel;
        this.notifier = notifier;
        getChildren()
                .addAll(
                        ToolLayout.hint(Messages.get("devutils.hash.generate.hint")),
                        buildSecretCard(),
                        buildKeyPairCard(),
                        buildUuidCard());
    }

    private VBox buildSecretCard() {
        TextField value = ToolLayout.codeField();
        value.setEditable(false);
        value.textProperty().bind(viewModel.secretProperty());
        HBox.setHgrow(value, Priority.ALWAYS);

        HBox options = ToolLayout.row(
                ToolLayout.labeled(
                        Messages.get("devutils.hash.generate.secret.length"),
                        ToolLayout.segmented(
                                viewModel.secretBytesProperty(),
                                GenerateSecretUseCase.BYTE_LENGTHS,
                                bytes -> Messages.format("devutils.hash.generate.secret.bytes", bytes))),
                ToolLayout.labeled(
                        Messages.get("devutils.hash.generate.secret.encoding"),
                        ToolLayout.segmented(
                                viewModel.secretEncodingProperty(),
                                List.of(SecretEncoding.values()),
                                encoding -> Messages.get(ENCODING_LABELS.get(encoding)))),
                ToolLayout.spacer(),
                regenerateButton(viewModel::regenerateSecret));
        options.setSpacing(ToolLayout.SPACING);
        return ToolLayout.card(
                Messages.get("devutils.hash.generate.secret.title"),
                List.of(ClipboardActions.clearButton(notifier)),
                options,
                ToolLayout.row(value, ClipboardActions.copyButton(value::getText, notifier)));
    }

    private VBox buildKeyPairCard() {
        ChoiceBox<KeyPairAlgorithm> algorithm = new ChoiceBox<>();
        algorithm.getItems().setAll(KeyPairAlgorithm.values());
        algorithm.setConverter(ToolLayout.labels(KeyPairAlgorithm::label));
        algorithm.valueProperty().bindBidirectional(viewModel.keyAlgorithmProperty());

        Button regenerate = regenerateButton(viewModel::regenerateKeyPair);
        regenerate.disableProperty().bind(viewModel.keyBusyProperty());
        HBox options = ToolLayout.row(
                ToolLayout.labeled(Messages.get("devutils.hash.generate.keyPair.algorithm"), algorithm),
                ToolLayout.spacer(),
                regenerate);

        TaskStatusBar status = new TaskStatusBar();
        viewModel.keyTaskProperty().addListener((observable, previous, task) -> status.track(task));
        Label generating = ToolLayout.inlineMessage(Tone.INFO);
        generating
                .textProperty()
                .bind(Bindings.when(viewModel.keyBusyProperty())
                        .then(Messages.get("devutils.hash.generate.keyPair.generating"))
                        .otherwise(""));

        Label warning = ToolLayout.inlineMessage(Tone.WARNING);
        warning.setText(Messages.get("devutils.hash.generate.keyPair.privateWarning"));

        return ToolLayout.card(
                Messages.get("devutils.hash.generate.keyPair.title"),
                List.of(ClipboardActions.clearButton(notifier)),
                options,
                generating,
                status,
                keySection(KeyPart.PUBLIC),
                warning,
                keySection(KeyPart.PRIVATE));
    }

    private VBox keySection(KeyPart part) {
        TextArea area = ToolLayout.codeArea(false);
        area.setWrapText(false);
        area.setPrefRowCount(KEY_ROWS);
        area.textProperty()
                .bind(Bindings.createStringBinding(() -> viewModel.keyText(part), viewModel.keyPairProperty()));

        BooleanBinding missing = viewModel.keyPairProperty().isNull();
        Button copy = ClipboardActions.copyButton(() -> viewModel.keyText(part), notifier);
        Button download = new Button(Messages.get("download.button"));
        download.setOnAction(event -> viewModel.download(part));
        Button saveAs = new Button(Messages.get("download.saveAs.button"));
        saveAs.setOnAction(event -> viewModel.saveAs(part));
        for (Button button : List.of(copy, download, saveAs)) {
            button.disableProperty().bind(missing);
        }
        HBox actions = ToolLayout.row(ToolLayout.spacer(), copy, ToolLayout.downloadButtons(download, saveAs));
        return ToolLayout.labeled(Messages.get(KEY_TITLES.get(part)), new VBox(ToolLayout.GAP, area, actions));
    }

    private VBox buildUuidCard() {
        Spinner<Integer> count = new Spinner<>(
                GenerateUuidsUseCase.MIN_COUNT,
                GenerateUuidsUseCase.MAX_COUNT,
                viewModel.uuidCountProperty().get());
        count.setEditable(true);
        count.setPrefWidth(SPINNER_WIDTH);
        SpinnerValueFactory<Integer> factory = count.getValueFactory();
        // Ketikan bukan angka dikembalikan ke nilai terakhir, bukan NumberFormatException; value factory
        // sendiri sudah meng-clamp angka di luar rentang
        factory.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return value == null ? "" : value.toString();
            }

            @Override
            public Integer fromString(String text) {
                try {
                    return Integer.valueOf(text.strip());
                } catch (NumberFormatException e) {
                    return factory.getValue();
                }
            }
        });
        factory.valueProperty().bindBidirectional(viewModel.uuidCountProperty());

        HBox options = ToolLayout.row(
                ToolLayout.labeled(
                        Messages.get("devutils.hash.generate.uuid.version"),
                        ToolLayout.segmented(
                                viewModel.uuidVersionProperty(),
                                List.of(UuidVersion.values()),
                                version -> Messages.get(UUID_LABELS.get(version)))),
                ToolLayout.labeled(Messages.get("devutils.hash.generate.uuid.count"), count),
                ToolLayout.spacer(),
                regenerateButton(viewModel::regenerateUuids));
        options.setSpacing(ToolLayout.SPACING);

        TextArea output = ToolLayout.codeArea(false);
        output.setWrapText(false);
        output.setPrefRowCount(UUID_ROWS);
        output.textProperty().bind(viewModel.uuidsProperty());
        return ToolLayout.card(
                Messages.get("devutils.hash.generate.uuid.title"),
                List.of(ClipboardActions.copyButton(output::getText, notifier)),
                options,
                output);
    }

    private static Button regenerateButton(Runnable action) {
        return ToolLayout.accentButton(Messages.get("devutils.hash.generate.regenerate"), action);
    }
}
