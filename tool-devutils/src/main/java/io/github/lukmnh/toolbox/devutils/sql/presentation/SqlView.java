package io.github.lukmnh.toolbox.devutils.sql.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.common.ui.TaskStatusBar;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.devutils.sql.application.FormatSqlUseCase;
import io.github.lukmnh.toolbox.devutils.sql.domain.FormatOptions;
import io.github.lukmnh.toolbox.devutils.sql.domain.IndentStyle;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlDialect;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/** Layout SQL Formatter: input, opsi + aksi Format/Minify, lalu hasil read-only dengan Copy/Download. */
final class SqlView {

    private static final double EDITOR_HEIGHT = 240;
    private static final double OUTPUT_HEIGHT = 360;
    private static final double SPINNER_WIDTH = 80;
    private static final Set<String> SQL_EXTENSIONS = Set.of("sql");

    private static final Map<IndentStyle, String> INDENT_LABELS = Map.of(
            IndentStyle.TWO_SPACES, "devutils.sql.indent.two",
            IndentStyle.FOUR_SPACES, "devutils.sql.indent.four",
            IndentStyle.TAB, "devutils.sql.indent.tab");

    private final SqlViewModel viewModel;
    private final ToolContext context;

    private SqlView(SqlViewModel viewModel, ToolContext context) {
        this.viewModel = viewModel;
        this.context = context;
    }

    static Node create(SqlViewModel viewModel, ToolContext context) {
        return new SqlView(viewModel, context).build();
    }

    private Node build() {
        TaskStatusBar status = new TaskStatusBar();
        viewModel.currentTaskProperty().addListener((observable, previous, task) -> status.track(task));
        VBox page = ToolLayout.page();
        page.getChildren()
                .addAll(
                        ToolLayout.header(
                                ToolCategory.DEV_UTILS,
                                Messages.get("devutils.sql.name"),
                                Messages.get("devutils.sql.hint")),
                        buildInput(),
                        buildOptions(),
                        status,
                        buildResult());
        page.addEventFilter(KeyEvent.KEY_PRESSED, this::handleShortcut);
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("edge-to-edge");
        return scroll;
    }

    /** Ctrl+Enter: Format. */
    private void handleShortcut(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER
                && event.isShortcutDown()
                && viewModel.canRun().get()) {
            viewModel.format();
            event.consume();
        }
    }

    private VBox buildInput() {
        TextArea area = ToolLayout.codeArea(true);
        area.setWrapText(false);
        area.setPrefHeight(EDITOR_HEIGHT);
        area.setPromptText(Messages.get("devutils.sql.input.prompt"));
        area.textProperty().bindBidirectional(viewModel.inputProperty());
        // File besar tidak dimuat ke editor; selama ada file besar, editor nonaktif
        area.disableProperty().bind(viewModel.largeFileProperty().isNotNull());

        Label info = ToolLayout.statText();
        info.textProperty()
                .bind(Bindings.createStringBinding(
                        viewModel::inputSummary, viewModel.fileNameProperty(), viewModel.largeFileProperty()));
        Button upload = ToolLayout.button(Messages.get("devutils.sql.upload"), this::chooseFile);
        Button clear = ToolLayout.button(Messages.get("devutils.sql.clear"), viewModel::clear);
        clear.disableProperty().bind(viewModel.isEmpty());

        VBox card = ToolLayout.card(Messages.get("devutils.sql.input"), List.of(upload, clear), area, info);
        ToolLayout.acceptDrops(
                card,
                SQL_EXTENSIONS,
                files -> viewModel.loadFile(files.getFirst()),
                names -> context.notifier().warning(Messages.format("devutils.drop.rejected", names)));
        return card;
    }

    private VBox buildOptions() {
        ChoiceBox<SqlDialect> dialect = new ChoiceBox<>();
        dialect.getItems().setAll(SqlDialect.values());
        dialect.setConverter(ToolLayout.labels(SqlDialect::displayName));
        dialect.valueProperty().bindBidirectional(viewModel.dialectProperty());

        CheckBox uppercase = new CheckBox(Messages.get("devutils.sql.uppercase"));
        uppercase.selectedProperty().bindBidirectional(viewModel.uppercaseKeywordsProperty());
        uppercase.setTooltip(new Tooltip(Messages.get("devutils.sql.uppercase.tooltip")));
        CheckBox removeComments = new CheckBox(Messages.get("devutils.sql.removeComments"));
        removeComments.selectedProperty().bindBidirectional(viewModel.removeCommentsProperty());
        removeComments.setTooltip(new Tooltip(Messages.get("devutils.sql.removeComments.tooltip")));

        Button minify = ToolLayout.button(Messages.get("devutils.sql.action.minify"), viewModel::minify);
        Button format = ToolLayout.accentButton(Messages.get("devutils.sql.action.format"), viewModel::format);
        format.setTooltip(new Tooltip(Messages.get("devutils.sql.format.tooltip")));
        minify.disableProperty().bind(viewModel.canRun().not());
        format.disableProperty().bind(viewModel.canRun().not());

        HBox formatRow = ToolLayout.row(
                ToolLayout.labeled(Messages.get("devutils.sql.dialect"), dialect),
                ToolLayout.labeled(
                        Messages.get("devutils.sql.indent"),
                        ToolLayout.segmented(
                                viewModel.indentProperty(),
                                List.of(IndentStyle.values()),
                                style -> Messages.get(INDENT_LABELS.get(style)))),
                ToolLayout.labeled(Messages.get("devutils.sql.blankLines"), blankLinesSpinner()),
                uppercase);
        formatRow.setSpacing(ToolLayout.SPACING);
        HBox actionRow = ToolLayout.row(removeComments, ToolLayout.spacer(), minify, format);
        return ToolLayout.card(formatRow, actionRow, ToolLayout.hint(Messages.get("devutils.sql.notValidator")));
    }

    private Spinner<Integer> blankLinesSpinner() {
        Spinner<Integer> spinner = new Spinner<>(
                0, FormatOptions.MAX_BLANK_LINES, viewModel.blankLinesProperty().get());
        spinner.setPrefWidth(SPINNER_WIDTH);
        spinner.getValueFactory().valueProperty().bindBidirectional(viewModel.blankLinesProperty());
        return spinner;
    }

    private VBox buildResult() {
        Label info = ToolLayout.statText();
        info.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> describe(viewModel.resultProperty().get()), viewModel.resultProperty()));

        TextArea output = ToolLayout.codeArea(false);
        output.setWrapText(false);
        output.setPrefHeight(OUTPUT_HEIGHT);
        output.textProperty().bind(viewModel.previewProperty());

        Button copy = ToolLayout.button(Messages.get("common.copy"), viewModel::copy);
        Button download = ToolLayout.accentButton(Messages.get("download.button"), viewModel::download);
        Button saveAs = ToolLayout.button(Messages.get("download.saveAs.button"), viewModel::saveAs);
        for (Button button : List.of(copy, download, saveAs)) {
            button.disableProperty().bind(viewModel.canExport().not());
        }

        VBox card = ToolLayout.card(
                Messages.get("devutils.sql.result"),
                List.of(),
                info,
                staleMessage(),
                unformattedMessage(),
                truncatedMessage(),
                output,
                ToolLayout.row(ToolLayout.spacer(), copy, saveAs, download));
        card.visibleProperty().bind(viewModel.resultProperty().isNotNull());
        ToolLayout.collapseWhenHidden(card);
        return card;
    }

    private static String describe(SqlRun run) {
        if (run == null) {
            return "";
        }
        return Messages.format(
                "devutils.sql.result.info",
                Messages.get(run.action().labelKey()),
                run.dialect().displayName(),
                ByteSizeFormat.format(run.inputBytes()),
                ByteSizeFormat.format(run.outputBytes()));
    }

    private Label staleMessage() {
        Label message = ToolLayout.inlineMessage(Tone.WARNING);
        message.textProperty()
                .bind(Bindings.when(viewModel.staleProperty())
                        .then(Messages.get("devutils.sql.stale"))
                        .otherwise(""));
        return message;
    }

    private Label unformattedMessage() {
        Label message = ToolLayout.inlineMessage(Tone.WARNING);
        message.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> {
                            SqlRun run = viewModel.resultProperty().get();
                            int skipped = run == null ? 0 : run.output().unformattedStatements();
                            return skipped == 0
                                    ? ""
                                    : Messages.format(
                                            "devutils.sql.unformatted", skipped, FormatSqlUseCase.MAX_STATEMENT_CHARS);
                        },
                        viewModel.resultProperty()));
        return message;
    }

    private Label truncatedMessage() {
        Label message = ToolLayout.inlineMessage(Tone.INFO);
        message.textProperty()
                .bind(Bindings.when(viewModel.previewTruncatedProperty())
                        .then(Messages.format("devutils.sql.previewTruncated", SqlViewModel.EDITOR_MAX_CHARS))
                        .otherwise(""));
        return message;
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Messages.get("devutils.sql.chooser.title"));
        chooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter(Messages.get("devutils.sql.chooser.filter"), "*.sql", "*.SQL"));
        File chosen = chooser.showOpenDialog(context.owner());
        if (chosen != null) {
            viewModel.loadFile(chosen.toPath());
        }
    }
}
