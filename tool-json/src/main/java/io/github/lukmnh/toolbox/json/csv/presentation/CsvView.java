package io.github.lukmnh.toolbox.json.csv.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.TaskStatusBar;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.json.csv.domain.CsvEncoding;
import io.github.lukmnh.toolbox.json.csv.domain.CsvSource;
import io.github.lukmnh.toolbox.json.csv.domain.Delimiter;
import io.github.lukmnh.toolbox.json.csv.domain.LineEnding;
import io.github.lukmnh.toolbox.json.csv.domain.MismatchPolicy;
import io.github.lukmnh.toolbox.json.csv.domain.OutputDelimiter;
import io.github.lukmnh.toolbox.json.csv.domain.PipelinePreview;
import io.github.lukmnh.toolbox.json.csv.domain.QuoteMode;
import java.io.File;
import java.util.List;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/** Layout CSV Split & Merge: file + format, pipeline | pratinjau, opsi hasil + ekspor. */
final class CsvView {

    private static final double WORK_AREA_MIN_HEIGHT = 460;
    private static final double PIPELINE_DIVIDER = 0.38;

    private final CsvViewModel viewModel;
    private final ToolContext context;

    private CsvView(CsvViewModel viewModel, ToolContext context) {
        this.viewModel = viewModel;
        this.context = context;
    }

    static Node create(CsvViewModel viewModel, ToolContext context) {
        return new CsvView(viewModel, context).build();
    }

    private Node build() {
        TaskStatusBar status = new TaskStatusBar();
        viewModel.currentTaskProperty().addListener((observable, previous, task) -> status.track(task));

        VBox page = ToolLayout.page();
        page.getChildren()
                .addAll(
                        ToolLayout.header(
                                ToolCategory.JSON, Messages.get("json.csv.name"), Messages.get("json.csv.hint")),
                        whenNoFile(dropCard()),
                        whenFile(fileCard()),
                        whenFile(workArea()),
                        whenFile(outputCard()),
                        status);
        ToolLayout.acceptDrops(
                page,
                CsvSource.EXTENSIONS,
                files -> viewModel.openFile(files.getFirst()),
                names -> context.notifier().warning(Messages.format("json.csv.drop.rejected", names)));

        // Halaman bisa lebih tinggi dari jendela kecil; fitToHeight tetap mengisi jendela besar
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("edge-to-edge");
        return scroll;
    }

    private VBox dropCard() {
        return ToolLayout.card(ToolLayout.dropZone(
                Messages.get("json.csv.drop.hint"), Messages.get("json.csv.chooseFile"), this::chooseFile));
    }

    private VBox fileCard() {
        Label info = ToolLayout.statText();
        info.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> viewModel.sourceProperty().get() == null
                                ? ""
                                : viewModel.fileSummary(
                                        viewModel.sourceProperty().get()),
                        viewModel.sourceProperty()));
        CheckBox header = new CheckBox(Messages.get("json.csv.format.header"));
        header.selectedProperty().bindBidirectional(viewModel.hasHeaderProperty());
        HBox format = ToolLayout.row(
                ToolLayout.labeled(
                        Messages.get("json.csv.format.delimiter"),
                        choice(viewModel.delimiterProperty(), List.of(Delimiter.values()), CsvLabels::delimiter)),
                ToolLayout.labeled(
                        Messages.get("json.csv.format.encoding"),
                        choice(viewModel.encodingProperty(), List.of(CsvEncoding.values()), CsvLabels::encoding)),
                ToolLayout.labeled(
                        Messages.get("json.csv.mismatch.title"),
                        ToolLayout.segmented(
                                viewModel.mismatchPolicyProperty(),
                                List.of(MismatchPolicy.values()),
                                CsvLabels::policy)),
                header);
        return ToolLayout.card(
                Messages.get("json.csv.file.title"),
                List.of(ToolLayout.button(Messages.get("json.csv.changeFile"), this::chooseFile)),
                info,
                format,
                ToolLayout.hint(Messages.get("json.csv.format.hint")));
    }

    private SplitPane workArea() {
        SplitPane split = new SplitPane(PipelinePane.create(viewModel), previewCard());
        split.getStyleClass().add("card-split");
        split.setDividerPositions(PIPELINE_DIVIDER);
        split.setMinHeight(WORK_AREA_MIN_HEIGHT);
        VBox.setVgrow(split, Priority.ALWAYS);
        return split;
    }

    private VBox previewCard() {
        PreviewTable output = new PreviewTable(Messages.get("json.csv.preview.empty"));
        PreviewTable input = new PreviewTable(Messages.get("json.csv.preview.empty"));
        viewModel
                .previewProperty()
                .addListener((observable, previous, current) -> output.show(current == null ? null : current.table()));
        viewModel.inputTableProperty().addListener((observable, previous, current) -> input.show(current));

        TabPane tabs = new TabPane(
                tab(Messages.get("json.csv.preview.result"), output),
                tab(Messages.get("json.csv.preview.input"), input));
        tabs.getStyleClass().add("underline-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        Label error = ToolLayout.inlineMessage(Tone.DANGER);
        error.textProperty().bind(viewModel.previewErrorProperty());
        MismatchPane mismatches = new MismatchPane("json.csv.mismatch.sample");
        viewModel
                .previewProperty()
                .addListener((observable, previous, current) ->
                        mismatches.show(current == null ? null : current.mismatches()));
        Label summary = ToolLayout.statText();
        summary.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> summaryText(viewModel.previewProperty().get()), viewModel.previewProperty()));

        VBox card = ToolLayout.card(Messages.get("json.csv.preview.title"), List.of(summary), tabs, error, mismatches);
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private String summaryText(PipelinePreview preview) {
        return preview == null ? "" : viewModel.previewSummary(preview);
    }

    private VBox outputCard() {
        CheckBox includeHeader = new CheckBox(Messages.get("json.csv.output.includeHeader"));
        includeHeader.selectedProperty().bindBidirectional(viewModel.includeHeaderProperty());
        CheckBox neutralize = new CheckBox(Messages.get("json.csv.output.neutralize"));
        neutralize.selectedProperty().bindBidirectional(viewModel.neutralizeFormulasProperty());
        Label neutralizeOff = ToolLayout.inlineMessage(Tone.WARNING);
        neutralizeOff
                .textProperty()
                .bind(Bindings.when(viewModel.neutralizeFormulasProperty())
                        .then("")
                        .otherwise(Messages.get("json.csv.output.neutralize.off")));

        Button run = ToolLayout.accentButton(Messages.get("json.csv.export.run"), viewModel::export);
        run.disableProperty().bind(viewModel.canExport().not());

        return ToolLayout.card(
                Messages.get("json.csv.output.title"),
                List.of(),
                ToolLayout.row(
                        ToolLayout.labeled(
                                Messages.get("json.csv.output.delimiter"),
                                choice(
                                        viewModel.outputDelimiterProperty(),
                                        List.of(OutputDelimiter.values()),
                                        CsvLabels::outputDelimiter)),
                        ToolLayout.labeled(
                                Messages.get("json.csv.output.encoding"),
                                ToolLayout.segmented(
                                        viewModel.outputEncodingProperty(),
                                        CsvEncoding.OUTPUT_CHOICES,
                                        CsvLabels::encoding)),
                        ToolLayout.labeled(
                                Messages.get("json.csv.output.quote"),
                                ToolLayout.segmented(
                                        viewModel.quoteProperty(), List.of(QuoteMode.values()), CsvLabels::quote)),
                        ToolLayout.labeled(
                                Messages.get("json.csv.output.lineEnding"),
                                ToolLayout.segmented(
                                        viewModel.lineEndingProperty(),
                                        List.of(LineEnding.values()),
                                        CsvLabels::lineEnding))),
                ToolLayout.row(includeHeader, neutralize),
                ToolLayout.hint(Messages.get("json.csv.output.neutralize.hint")),
                neutralizeOff,
                ToolLayout.row(ToolLayout.spacer(), run),
                resultBox());
    }

    private VBox resultBox() {
        Label summary = ToolLayout.statText();
        summary.setWrapText(true);
        summary.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> viewModel.resultProperty().get() == null
                                ? ""
                                : viewModel.resultSummary(
                                        viewModel.resultProperty().get()),
                        viewModel.resultProperty()));
        MismatchPane mismatches = new MismatchPane("json.csv.mismatch.file");
        viewModel
                .resultProperty()
                .addListener((observable, previous, current) ->
                        mismatches.show(current == null ? null : current.mismatches()));
        HBox actions = ToolLayout.row(
                summary, ToolLayout.spacer(), ToolLayout.downloadButtons(viewModel::download, viewModel::saveAs));
        VBox box = new VBox(ToolLayout.GAP, actions, mismatches);
        box.visibleProperty().bind(viewModel.resultProperty().isNotNull());
        ToolLayout.collapseWhenHidden(box);
        return box;
    }

    private <T extends Node> T whenFile(T node) {
        node.visibleProperty().bind(viewModel.sourceProperty().isNotNull());
        ToolLayout.collapseWhenHidden(node);
        return node;
    }

    private <T extends Node> T whenNoFile(T node) {
        node.visibleProperty().bind(viewModel.sourceProperty().isNull());
        ToolLayout.collapseWhenHidden(node);
        return node;
    }

    private static <T> ChoiceBox<T> choice(ObjectProperty<T> value, List<T> options, Function<T, String> label) {
        ChoiceBox<T> box = new ChoiceBox<>();
        box.getItems().setAll(options);
        box.setConverter(ToolLayout.labels(label));
        box.valueProperty().bindBidirectional(value);
        return box;
    }

    private static Tab tab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Messages.get("json.csv.chooser.title"));
        chooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter(
                        Messages.get("json.csv.chooser.filter"), "*.csv", "*.tsv", "*.txt", "*.CSV", "*.TSV", "*.TXT"));
        File chosen = chooser.showOpenDialog(context.owner());
        if (chosen != null) {
            viewModel.openFile(chosen.toPath());
        }
    }
}
