package io.github.lukmnh.toolbox.devutils.diff.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.TaskStatusBar;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffResult;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffRow;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffStats;
import io.github.lukmnh.toolbox.devutils.diff.domain.WhitespaceMode;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/** Layout Text Diff: dua input (Asli/Baru), opsi, lalu hasil side-by-side atau unified. */
final class DiffView {

    private static final double EDITOR_HEIGHT = 200;
    private static final double RESULT_HEIGHT = 480;
    /** Tinggi tetap membuat ListView virtual tidak perlu mengukur tiap baris (file 100 ribu baris tetap lancar). */
    private static final double ROW_HEIGHT = 22;
    /** Baris di atas perubahan yang ikut terlihat saat navigasi, supaya konteksnya terbaca. */
    private static final int SCROLL_CONTEXT_ROWS = 3;

    private static final Map<WhitespaceMode, String> WHITESPACE_LABELS = Map.of(
            WhitespaceMode.EXACT, "devutils.diff.whitespace.exact",
            WhitespaceMode.IGNORE_ALL, "devutils.diff.whitespace.all",
            WhitespaceMode.IGNORE_TRAILING, "devutils.diff.whitespace.trailing");
    private static final Map<DiffViewMode, String> MODE_LABELS = Map.of(
            DiffViewMode.SIDE_BY_SIDE, "devutils.diff.mode.sideBySide",
            DiffViewMode.UNIFIED, "devutils.diff.mode.unified");

    private final DiffViewModel viewModel;
    private final ToolContext context;

    private DiffView(DiffViewModel viewModel, ToolContext context) {
        this.viewModel = viewModel;
        this.context = context;
    }

    static Node create(DiffViewModel viewModel, ToolContext context) {
        return new DiffView(viewModel, context).build();
    }

    private Node build() {
        TaskStatusBar status = new TaskStatusBar();
        viewModel.currentTaskProperty().addListener((observable, previous, task) -> status.track(task));
        VBox page = ToolLayout.page();
        page.getChildren()
                .addAll(
                        ToolLayout.header(
                                ToolCategory.DEV_UTILS,
                                Messages.get("devutils.diff.name"),
                                Messages.get("devutils.diff.hint")),
                        buildInputs(),
                        buildOptions(),
                        status,
                        buildResult());
        page.addEventFilter(KeyEvent.KEY_PRESSED, this::handleShortcut);
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("edge-to-edge");
        return scroll;
    }

    /** F7 / Shift+F7: perubahan berikutnya / sebelumnya. Ctrl+Enter: bandingkan. */
    private void handleShortcut(KeyEvent event) {
        if (event.getCode() == KeyCode.F7) {
            if (event.isShiftDown()) {
                viewModel.previous();
            } else {
                viewModel.next();
            }
            event.consume();
            return;
        }
        if (event.getCode() == KeyCode.ENTER
                && event.isShortcutDown()
                && viewModel.canCompare().get()) {
            viewModel.compare();
            event.consume();
        }
    }

    private HBox buildInputs() {
        Button swap = ToolLayout.button(Messages.get("devutils.diff.swap"), viewModel::swap);
        swap.setTooltip(new Tooltip(Messages.get("devutils.diff.swap.tooltip")));
        VBox swapColumn = new VBox(swap);
        swapColumn.setAlignment(Pos.CENTER);
        VBox oldCard = inputCard(Messages.get("devutils.diff.input.old"), viewModel.left());
        VBox newCard = inputCard(Messages.get("devutils.diff.input.new"), viewModel.right());
        HBox.setHgrow(oldCard, Priority.ALWAYS);
        HBox.setHgrow(newCard, Priority.ALWAYS);
        oldCard.setPrefWidth(0);
        newCard.setPrefWidth(0);
        return new HBox(ToolLayout.GAP, oldCard, swapColumn, newCard);
    }

    private VBox inputCard(String title, DiffInput input) {
        TextArea area = ToolLayout.codeArea(true);
        area.setWrapText(false);
        area.setPrefHeight(EDITOR_HEIGHT);
        area.setPromptText(Messages.get("devutils.diff.input.prompt"));
        area.textProperty().bindBidirectional(input.textProperty());
        // File besar tidak dimuat ke editor; selama ada file besar, editor nonaktif
        area.disableProperty().bind(input.largeFileProperty().isNotNull());

        Label info = ToolLayout.statText();
        info.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> viewModel.fileSummary(input), input.fileNameProperty(), input.largeFileProperty()));
        Button upload = ToolLayout.button(Messages.get("devutils.diff.upload"), () -> chooseFile(input));
        Button clear = ToolLayout.button(Messages.get("devutils.diff.clear"), input::clear);
        clear.disableProperty().bind(input.isEmpty());

        VBox card = ToolLayout.card(title, List.of(upload, clear), area, info);
        ToolLayout.acceptDrops(
                card,
                Set.of(),
                files -> viewModel.loadFile(input, files.getFirst()),
                names -> context.notifier().warning(Messages.format("devutils.drop.rejected", names)));
        return card;
    }

    private VBox buildOptions() {
        CheckBox ignoreCase = new CheckBox(Messages.get("devutils.diff.ignoreCase"));
        ignoreCase.selectedProperty().bindBidirectional(viewModel.ignoreCaseProperty());
        CheckBox lineEndings = new CheckBox(Messages.get("devutils.diff.normalizeLineEndings"));
        lineEndings.selectedProperty().bindBidirectional(viewModel.normalizeLineEndingsProperty());
        lineEndings.setTooltip(new Tooltip(Messages.get("devutils.diff.normalizeLineEndings.tooltip")));
        Button compare = ToolLayout.accentButton(Messages.get("devutils.diff.compare"), viewModel::compare);
        compare.disableProperty().bind(viewModel.canCompare().not());
        compare.setTooltip(new Tooltip(Messages.get("devutils.diff.compare.tooltip")));

        HBox row = ToolLayout.row(
                new Label(Messages.get("devutils.diff.whitespace")),
                ToolLayout.segmented(
                        viewModel.whitespaceProperty(),
                        List.of(WhitespaceMode.EXACT, WhitespaceMode.IGNORE_TRAILING, WhitespaceMode.IGNORE_ALL),
                        mode -> Messages.get(WHITESPACE_LABELS.get(mode))),
                ignoreCase,
                lineEndings,
                ToolLayout.spacer(),
                compare);
        row.setSpacing(ToolLayout.SPACING);
        return ToolLayout.card(row);
    }

    private VBox buildResult() {
        Label added = ToolLayout.badge("", Tone.SUCCESS);
        Label deleted = ToolLayout.badge("", Tone.DANGER);
        Label changed = ToolLayout.badge("", Tone.WARNING);
        viewModel
                .resultProperty()
                .addListener((observable, previous, result) -> updateBadges(result, added, deleted, changed));

        Button previous = ToolLayout.button(Messages.get("devutils.diff.previous"), viewModel::previous);
        previous.setTooltip(new Tooltip(Messages.get("devutils.diff.previous.tooltip")));
        Button next = ToolLayout.button(Messages.get("devutils.diff.next"), viewModel::next);
        next.setTooltip(new Tooltip(Messages.get("devutils.diff.next.tooltip")));
        previous.disableProperty().bind(viewModel.hasChanges().not());
        next.disableProperty().bind(viewModel.hasChanges().not());
        HBox toolbar = ToolLayout.row(
                added,
                deleted,
                changed,
                ToolLayout.spacer(),
                ToolLayout.segmented(
                        viewModel.viewModeProperty(),
                        List.of(DiffViewMode.SIDE_BY_SIDE, DiffViewMode.UNIFIED),
                        mode -> Messages.get(MODE_LABELS.get(mode))),
                previous,
                next);

        VBox card = ToolLayout.card(
                Messages.get("devutils.diff.result"),
                List.of(),
                toolbar,
                identicalMessage(),
                staleMessage(),
                buildLists(),
                buildExportRow());
        card.visibleProperty().bind(viewModel.resultProperty().isNotNull());
        ToolLayout.collapseWhenHidden(card);
        return card;
    }

    private static void updateBadges(DiffResult result, Label added, Label deleted, Label changed) {
        if (result == null) {
            return;
        }
        DiffStats stats = result.stats();
        added.setText(Messages.format("devutils.diff.stats.added", stats.added()));
        deleted.setText(Messages.format("devutils.diff.stats.deleted", stats.deleted()));
        changed.setText(Messages.format("devutils.diff.stats.changed", stats.changed()));
    }

    private Label identicalMessage() {
        Label message = ToolLayout.inlineMessage(Tone.SUCCESS);
        message.textProperty()
                .bind(Bindings.when(viewModel
                                .resultProperty()
                                .isNotNull()
                                .and(viewModel.hasChanges().not()))
                        .then(Messages.get("devutils.diff.identical"))
                        .otherwise(""));
        return message;
    }

    private Label staleMessage() {
        Label message = ToolLayout.inlineMessage(Tone.WARNING);
        message.textProperty()
                .bind(Bindings.when(viewModel.staleProperty())
                        .then(Messages.get("devutils.diff.stale"))
                        .otherwise(""));
        return message;
    }

    private StackPane buildLists() {
        ListView<DiffRow> sideBySide = new ListView<>(viewModel.rows());
        sideBySide.setCellFactory(list -> new SideBySideCell());
        ListView<UnifiedLine> unified = new ListView<>(viewModel.unifiedLines());
        unified.setCellFactory(list -> new UnifiedCell());
        for (ListView<?> list : List.of(sideBySide, unified)) {
            list.getStyleClass().add("diff-list");
            list.setFixedCellSize(ROW_HEIGHT);
            list.setPlaceholder(new Label(Messages.get("devutils.diff.noLines")));
        }
        sideBySide.visibleProperty().bind(viewModel.viewModeProperty().isEqualTo(DiffViewMode.SIDE_BY_SIDE));
        unified.visibleProperty().bind(sideBySide.visibleProperty().not());
        viewModel.focusIndexProperty().addListener((observable, previous, index) -> {
            int target = index.intValue();
            if (target != DiffViewModel.NO_FOCUS) {
                ListView<?> active = sideBySide.isVisible() ? sideBySide : unified;
                active.scrollTo(Math.max(0, target - SCROLL_CONTEXT_ROWS));
                active.getSelectionModel().clearAndSelect(target);
            }
        });
        StackPane lists = new StackPane(sideBySide, unified);
        lists.setPrefHeight(RESULT_HEIGHT);
        VBox.setVgrow(lists, Priority.ALWAYS);
        return lists;
    }

    private HBox buildExportRow() {
        Label hint = ToolLayout.statText();
        hint.setText(Messages.get("devutils.diff.shortcuts"));
        Button copy = ToolLayout.button(Messages.get("devutils.diff.copyUnified"), viewModel::copyUnified);
        Button download = ToolLayout.accentButton(Messages.get("download.button"), viewModel::download);
        Button saveAs = ToolLayout.button(Messages.get("download.saveAs.button"), viewModel::saveAs);
        for (Button button : List.of(copy, download, saveAs)) {
            button.disableProperty().bind(viewModel.canExport().not());
        }
        return ToolLayout.row(hint, ToolLayout.spacer(), copy, saveAs, download);
    }

    private void chooseFile(DiffInput input) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Messages.get("devutils.diff.chooser.title"));
        File chosen = chooser.showOpenDialog(context.owner());
        if (chosen != null) {
            viewModel.loadFile(input, chosen.toPath());
        }
    }
}
