package io.github.lukmnh.toolbox.devutils.checksum.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.Notifier;
import io.github.lukmnh.toolbox.devutils.checksum.domain.FileVerification;
import io.github.lukmnh.toolbox.devutils.checksum.domain.SumsVerification;
import io.github.lukmnh.toolbox.devutils.checksum.domain.VerifyStatus;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevLayout;
import io.github.lukmnh.toolbox.devutils.shared.presentation.Tone;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** Tab Verifikasi: (a) satu file + checksum dengan indikator besar COCOK/TIDAK COCOK, (b) file daftar SUMS. */
final class VerifyPane extends ScrollPane {

    private static final String VERDICT = "verdict";
    private static final String VERDICT_MATCH = "verdict-match";
    private static final String VERDICT_MISMATCH = "verdict-mismatch";
    private static final double SUMS_TABLE_HEIGHT = 260;
    private static final double NAME_COLUMN_WIDTH = 220;
    private static final double STATUS_COLUMN_WIDTH = 150;
    private static final double CHECKSUM_COLUMN_WIDTH = 420;

    private final VerifyViewModel viewModel;
    private final Window owner;
    private final Notifier notifier;

    VerifyPane(VerifyViewModel viewModel, Window owner, Notifier notifier) {
        this.viewModel = viewModel;
        this.owner = owner;
        this.notifier = notifier;
        setFitToWidth(true);
        getStyleClass().add("edge-to-edge");
        VBox content = new VBox(DevLayout.SPACING, buildSingleFile(), buildSumsFile());
        content.getStyleClass().add("tool-view");
        content.setPadding(new Insets(DevLayout.SPACING, 0, 0, 0));
        setContent(content);
    }

    // ---------- (a) Satu file ----------

    private VBox buildSingleFile() {
        Label fileName = DevLayout.statText();
        fileName.textProperty().bind(fileLabel(viewModel.fileProperty(), "devutils.checksum.verify.file.none"));
        Button choose = DevLayout.button(
                Messages.get("devutils.checksum.chooseFile"),
                () -> chooseFile(Messages.get("devutils.checksum.chooseFile.title"), viewModel.fileProperty()::set));

        TextField expected = DevLayout.codeField();
        expected.setPromptText(Messages.get("devutils.checksum.verify.expected.prompt"));
        expected.textProperty().bindBidirectional(viewModel.expectedProperty());
        HBox.setHgrow(expected, Priority.ALWAYS);

        Label detected = DevLayout.badge("", Tone.INFO);
        detected.textProperty().bind(viewModel.detectedAlgorithm());
        detected.visibleProperty().bind(detected.textProperty().isNotEmpty());
        DevLayout.collapseWhenHidden(detected);

        Button verify = DevLayout.accentButton(Messages.get("devutils.checksum.verify.run"), viewModel::verifyFile);
        verify.disableProperty().bind(viewModel.canVerifyFile().not());

        Label error = DevLayout.inlineMessage(Tone.DANGER);
        error.textProperty().bind(viewModel.fileErrorProperty());

        VBox card = DevLayout.card(
                Messages.get("devutils.checksum.verify.single"),
                List.of(),
                DevLayout.hint(Messages.get("devutils.checksum.verify.single.hint")),
                DevLayout.row(choose, fileName),
                DevLayout.labeled(Messages.get("devutils.checksum.verify.expected"), expected),
                DevLayout.row(
                        DevLayout.labeled(
                                Messages.get("devutils.checksum.algorithm"), choiceBox(viewModel.fileChoiceProperty())),
                        detected,
                        DevLayout.spacer(),
                        verify),
                buildVerdict(),
                error);
        DevLayout.acceptDrops(
                card, Set.of(), files -> viewModel.fileProperty().set(files.getFirst()), this::warnRejected);
        return card;
    }

    private VBox buildVerdict() {
        Label verdict = new Label();
        verdict.getStyleClass().add(VERDICT);
        verdict.setMaxWidth(Double.MAX_VALUE);
        Label detail = DevLayout.statText();
        detail.getStyleClass().add("mono");
        detail.setWrapText(true);
        viewModel
                .fileResultProperty()
                .addListener((observable, previous, result) -> showVerdict(verdict, detail, result));
        VBox box = new VBox(6, verdict, detail);
        box.visibleProperty().bind(viewModel.fileResultProperty().isNotNull());
        DevLayout.collapseWhenHidden(box);
        return box;
    }

    private static void showVerdict(Label verdict, Label detail, FileVerification result) {
        verdict.getStyleClass().removeAll(VERDICT_MATCH, VERDICT_MISMATCH);
        if (result == null) {
            return;
        }
        boolean matches = result.matches();
        verdict.getStyleClass().add(matches ? VERDICT_MATCH : VERDICT_MISMATCH);
        verdict.setText(
                Messages.get(matches ? "devutils.checksum.verdict.match" : "devutils.checksum.verdict.mismatch"));
        detail.setText(Messages.format(
                "devutils.checksum.verdict.detail",
                result.fileName(),
                result.algorithm().label(),
                result.expected(),
                result.actual()));
    }

    // ---------- (b) File daftar SUMS ----------

    private VBox buildSumsFile() {
        Label fileName = DevLayout.statText();
        fileName.textProperty().bind(fileLabel(viewModel.sumsFileProperty(), "devutils.checksum.verify.sums.none"));
        Button choose = DevLayout.accentButton(
                Messages.get("devutils.checksum.verify.sums.choose"),
                () -> chooseFile(Messages.get("devutils.checksum.verify.sums.choose.title"), viewModel::verifySums));
        choose.disableProperty().bind(viewModel.idle().not());

        ChoiceBox<AlgorithmChoice> algorithm = choiceBox(viewModel.sumsChoiceProperty());
        // Ganti algoritma setelah file dimuat → verifikasi ulang tanpa memilih file lagi
        viewModel.sumsChoiceProperty().addListener((observable, previous, current) -> viewModel.reverifySums());

        Label error = DevLayout.inlineMessage(Tone.DANGER);
        error.textProperty().bind(viewModel.sumsErrorProperty());

        TableView<SumsVerification> table = buildSumsTable();
        table.setPrefHeight(SUMS_TABLE_HEIGHT);

        VBox card = DevLayout.card(
                Messages.get("devutils.checksum.verify.sums"),
                List.of(),
                DevLayout.hint(Messages.get("devutils.checksum.verify.sums.hint")),
                DevLayout.row(
                        choose,
                        fileName,
                        DevLayout.spacer(),
                        DevLayout.labeled(Messages.get("devutils.checksum.algorithm"), algorithm)),
                buildCounters(),
                error,
                table);
        DevLayout.acceptDrops(card, Set.of(), files -> viewModel.verifySums(files.getFirst()), this::warnRejected);
        return card;
    }

    /** Ringkasan per status sebagai badge berwarna: OK 12 · GAGAL 1 · …. */
    private HBox buildCounters() {
        HBox row = DevLayout.row();
        viewModel.sumsResults().addListener((ListChangeListener<SumsVerification>) change -> {
            row.getChildren().clear();
            if (viewModel.sumsResults().isEmpty()) {
                return;
            }
            for (VerifyStatus status : VerifyStatus.values()) {
                row.getChildren()
                        .add(DevLayout.badge(
                                Messages.format(
                                        "devutils.checksum.count", statusLabel(status), viewModel.count(status)),
                                tone(status)));
            }
        });
        row.visibleProperty().bind(Bindings.isNotEmpty(viewModel.sumsResults()));
        DevLayout.collapseWhenHidden(row);
        return row;
    }

    private TableView<SumsVerification> buildSumsTable() {
        TableView<SumsVerification> table = new TableView<>(viewModel.sumsResults());
        table.setPlaceholder(DevLayout.hint(Messages.get("devutils.checksum.verify.sums.empty")));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<SumsVerification, VerifyStatus> status =
                new TableColumn<>(Messages.get("devutils.checksum.column.status"));
        status.setPrefWidth(STATUS_COLUMN_WIDTH);
        status.setCellValueFactory(
                cell -> new ReadOnlyObjectWrapper<>(cell.getValue().status()));
        status.setCellFactory(column -> statusCell());
        table.getColumns()
                .setAll(List.of(
                        GeneratePane.column(
                                "devutils.checksum.column.file", NAME_COLUMN_WIDTH, SumsVerification::fileName, false),
                        status,
                        GeneratePane.column(
                                "devutils.checksum.column.expected",
                                CHECKSUM_COLUMN_WIDTH,
                                SumsVerification::expected,
                                true)));
        return table;
    }

    private static TableCell<SumsVerification, VerifyStatus> statusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(VerifyStatus item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : DevLayout.badge(statusLabel(item), tone(item)));
            }
        };
    }

    private static String statusLabel(VerifyStatus status) {
        return Messages.get("devutils.checksum.status." + status.name());
    }

    private static Tone tone(VerifyStatus status) {
        return switch (status) {
            case OK -> Tone.SUCCESS;
            case FAILED -> Tone.DANGER;
            case NOT_FOUND -> Tone.NEUTRAL;
            case REJECTED -> Tone.WARNING;
        };
    }

    // ---------- Bersama ----------

    private static ChoiceBox<AlgorithmChoice> choiceBox(ObjectProperty<AlgorithmChoice> value) {
        ChoiceBox<AlgorithmChoice> choice = new ChoiceBox<>();
        choice.getItems().setAll(AlgorithmChoice.values());
        choice.setConverter(DevLayout.labels(AlgorithmChoice::label));
        choice.valueProperty().bindBidirectional(value);
        return choice;
    }

    private static StringBinding fileLabel(ReadOnlyObjectProperty<Path> file, String noneKey) {
        return Bindings.createStringBinding(
                () -> file.get() == null
                        ? Messages.get(noneKey)
                        : file.get().getFileName().toString(),
                file);
    }

    private void warnRejected(String names) {
        notifier.warning(Messages.format("devutils.drop.rejected", names));
    }

    private void chooseFile(String title, Consumer<Path> onChosen) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        File chosen = chooser.showOpenDialog(owner);
        if (chosen != null) {
            onChosen.accept(chosen.toPath());
        }
    }
}
