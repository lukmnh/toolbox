package io.github.lukmnh.toolbox.productivity.text.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.TaskStatusBar;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.productivity.shared.domain.CaseStyle;
import io.github.lukmnh.toolbox.productivity.text.domain.TextAnalyzer;
import io.github.lukmnh.toolbox.productivity.text.domain.TextHistory;
import io.github.lukmnh.toolbox.productivity.text.domain.TextStatistics;
import io.github.lukmnh.toolbox.productivity.text.domain.TidyOperation;
import io.github.lukmnh.toolbox.productivity.text.domain.WordFrequency;
import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/** Layout Text Tools: editor + batas karakter, statistik & kata terbanyak, lalu ubah huruf / rapikan / ekspor. */
final class TextView {

    private static final double EDITOR_HEIGHT = 280;
    private static final double LIMIT_FIELD_WIDTH = 120;
    private static final double TOP_WORDS_WIDTH = 280;
    private static final Set<String> TEXT_EXTENSIONS = Set.of("txt");

    private final TextViewModel viewModel;
    private final ToolContext context;

    private TextView(TextViewModel viewModel, ToolContext context) {
        this.viewModel = viewModel;
        this.context = context;
    }

    static Node create(TextViewModel viewModel, ToolContext context) {
        return new TextView(viewModel, context).build();
    }

    private Node build() {
        TaskStatusBar status = new TaskStatusBar();
        viewModel.currentTaskProperty().addListener((observable, previous, task) -> status.track(task));
        VBox statistics = buildStatistics();
        HBox.setHgrow(statistics, Priority.ALWAYS);
        HBox insights = new HBox(ToolLayout.SPACING, statistics, buildTopWords());
        VBox page = ToolLayout.page();
        page.getChildren()
                .addAll(
                        ToolLayout.header(
                                ToolCategory.PRODUCTIVITY,
                                Messages.get("productivity.text.name"),
                                Messages.get("productivity.text.hint")),
                        buildInput(),
                        status,
                        insights,
                        buildTransform());
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("edge-to-edge");
        return scroll;
    }

    private VBox buildInput() {
        TextArea area = new TextArea();
        area.setWrapText(true);
        area.setPrefHeight(EDITOR_HEIGHT);
        area.setPromptText(Messages.get("productivity.text.input.prompt"));
        area.setTextFormatter(new TextFormatter<String>(this::guardSize));
        area.textProperty().bindBidirectional(viewModel.textProperty());
        // Dikunci selama transformasi supaya ketikan tidak tertimpa hasil yang dihitung dari teks lama
        area.editableProperty().bind(viewModel.busyProperty().not());

        Button load = ToolLayout.button(Messages.get("productivity.text.load"), this::chooseFile);
        Button undo = ToolLayout.button(Messages.get("productivity.text.undo"), viewModel::undo);
        undo.disableProperty().bind(viewModel.canUndoProperty().not().or(viewModel.busyProperty()));
        Button clear = ToolLayout.button(Messages.get("productivity.text.clear"), viewModel::clear);
        clear.disableProperty().bind(viewModel.canTransform().not());
        load.disableProperty().bind(viewModel.busyProperty());

        VBox card = ToolLayout.card(
                Messages.get("productivity.text.input"), List.of(load, undo, clear), area, buildLimitRow());
        ToolLayout.acceptDrops(
                card,
                TEXT_EXTENSIONS,
                files -> viewModel.loadFile(files.getFirst()),
                names -> context.notifier().warning(Messages.format("productivity.text.drop.rejected", names)));
        return card;
    }

    /** Edit yang hanya menghapus tidak mungkin melewati batas, jadi teks baru tidak perlu dibangun. */
    private TextFormatter.Change guardSize(TextFormatter.Change change) {
        if (change.getText().isEmpty() || viewModel.accepts(change.getControlNewText())) {
            return change;
        }
        return null;
    }

    private HBox buildLimitRow() {
        TextField limit = new TextField();
        limit.setPromptText(Messages.get("productivity.text.limit.prompt"));
        limit.setPrefWidth(LIMIT_FIELD_WIDTH);
        limit.textProperty().bindBidirectional(viewModel.characterLimitProperty());
        Label caption = new Label(Messages.get("productivity.text.limit"));
        caption.getStyleClass().add("field-label");

        Label badge = ToolLayout.badge("", Tone.NEUTRAL);
        viewModel.limitIndicatorProperty().addListener((observable, previous, current) -> {
            ToolLayout.update(badge, current.text(), current.tone());
        });
        badge.visibleProperty().bind(badge.textProperty().isNotEmpty());
        ToolLayout.collapseWhenHidden(badge);

        Label analyzing = ToolLayout.statText();
        analyzing.setText(Messages.get("productivity.text.analyzing"));
        analyzing.visibleProperty().bind(viewModel.analyzingProperty());
        return ToolLayout.row(caption, limit, badge, ToolLayout.spacer(), analyzing);
    }

    private VBox buildStatistics() {
        FlowPane tiles = new FlowPane(ToolLayout.SPACING, ToolLayout.SPACING);
        tiles.getChildren()
                .addAll(
                        tile("productivity.text.stat.characters", stats -> number(stats.characters())),
                        tile(
                                "productivity.text.stat.charactersNoSpaces",
                                stats -> number(stats.charactersWithoutSpaces())),
                        tile("productivity.text.stat.words", stats -> number(stats.words())),
                        tile("productivity.text.stat.sentences", stats -> number(stats.sentences())),
                        tile("productivity.text.stat.paragraphs", stats -> number(stats.paragraphs())),
                        tile("productivity.text.stat.lines", stats -> number(stats.lines())),
                        tile("productivity.text.stat.reading", stats -> duration(stats.readingTime())),
                        tile("productivity.text.stat.speaking", stats -> duration(stats.speakingTime())));
        return ToolLayout.card(
                Messages.get("productivity.text.stats"),
                List.of(),
                tiles,
                ToolLayout.hint(Messages.format(
                        "productivity.text.stats.hint",
                        TextAnalyzer.READING_WORDS_PER_MINUTE,
                        TextAnalyzer.SPEAKING_WORDS_PER_MINUTE)));
    }

    private VBox tile(String captionKey, Function<TextStatistics, String> value) {
        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("stat-value");
        valueLabel
                .textProperty()
                .bind(Bindings.createStringBinding(
                        () -> value.apply(viewModel.statisticsProperty().get()), viewModel.statisticsProperty()));
        Label caption = new Label(Messages.get(captionKey));
        caption.getStyleClass().add("field-label");
        VBox tile = new VBox(2, valueLabel, caption);
        tile.getStyleClass().add("stat-tile");
        return tile;
    }

    private VBox buildTopWords() {
        CheckBox ignore = new CheckBox(Messages.get("productivity.text.topWords.ignore"));
        ignore.selectedProperty().bindBidirectional(viewModel.ignoreStopWordsProperty());
        VBox rows = new VBox(2);
        Label empty = ToolLayout.hint(Messages.get("productivity.text.topWords.empty"));
        empty.visibleProperty().bind(Bindings.isEmpty(rows.getChildren()));
        ToolLayout.collapseWhenHidden(empty);
        viewModel
                .statisticsProperty()
                .addListener(
                        (observable, previous, current) -> rows.getChildren().setAll(topWordRows(current.topWords())));
        VBox card = ToolLayout.card(
                Messages.format("productivity.text.topWords", TextAnalyzer.TOP_WORD_LIMIT),
                List.of(),
                ignore,
                empty,
                rows);
        card.setPrefWidth(TOP_WORDS_WIDTH);
        card.setMinWidth(TOP_WORDS_WIDTH);
        return card;
    }

    private static List<Node> topWordRows(List<WordFrequency> words) {
        return IntStream.range(0, words.size())
                .<Node>mapToObj(index -> topWordRow(index + 1, words.get(index)))
                .toList();
    }

    private static HBox topWordRow(int rank, WordFrequency frequency) {
        Label rankLabel = ToolLayout.statText();
        rankLabel.setText(Messages.format("productivity.text.topWords.rank", rank));
        Label word = new Label(frequency.word());
        Label count = ToolLayout.statText();
        count.setText(Messages.format("productivity.text.topWords.count", frequency.count()));
        return ToolLayout.row(rankLabel, word, ToolLayout.spacer(), count);
    }

    private VBox buildTransform() {
        ChoiceBox<CaseStyle> style = new ChoiceBox<>();
        style.getItems().setAll(CaseStyle.values());
        style.setConverter(ToolLayout.labels(value -> Messages.get(labelKey("productivity.text.case.", value))));
        style.valueProperty().bindBidirectional(viewModel.caseStyleProperty());
        CheckBox indonesian = new CheckBox(Messages.get("productivity.text.case.indonesianTitle"));
        indonesian.selectedProperty().bindBidirectional(viewModel.indonesianTitleProperty());
        indonesian.disableProperty().bind(viewModel.caseStyleProperty().isNotEqualTo(CaseStyle.TITLE));
        Button applyCase = ToolLayout.accentButton(Messages.get("productivity.text.case.apply"), viewModel::applyCase);
        applyCase.disableProperty().bind(viewModel.canTransform().not());
        HBox caseRow = ToolLayout.row(
                ToolLayout.labeled(Messages.get("productivity.text.case"), style), indonesian, applyCase);
        caseRow.setAlignment(Pos.BOTTOM_LEFT);

        FlowPane tidyButtons = new FlowPane(ToolLayout.GAP, ToolLayout.GAP);
        for (TidyOperation operation : TidyOperation.values()) {
            Button button = ToolLayout.button(
                    Messages.get(labelKey("productivity.text.tidy.", operation)), () -> viewModel.tidy(operation));
            button.disableProperty().bind(viewModel.canTransform().not());
            tidyButtons.getChildren().add(button);
        }

        Button copy = ToolLayout.button(Messages.get("common.copy"), viewModel::copy);
        HBox download = ToolLayout.downloadButtons(viewModel::download, viewModel::saveAs);
        copy.disableProperty().bind(viewModel.canTransform().not());
        download.disableProperty().bind(viewModel.canTransform().not());

        return ToolLayout.card(
                Messages.get("productivity.text.transform"),
                List.of(),
                caseRow,
                ToolLayout.labeled(Messages.get("productivity.text.tidy"), tidyButtons),
                ToolLayout.hint(Messages.format("productivity.text.undo.hint", TextHistory.MAX_STEPS)),
                ToolLayout.row(ToolLayout.spacer(), copy, download));
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Messages.get("productivity.text.chooser.title"));
        chooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter(
                        Messages.get("productivity.text.chooser.filter"), "*.txt", "*.TXT"));
        File chosen = chooser.showOpenDialog(context.owner());
        if (chosen != null) {
            viewModel.loadFile(chosen.toPath());
        }
    }

    private static String number(long value) {
        return Messages.format("productivity.text.stat.number", value);
    }

    private static String duration(Duration duration) {
        long hours = duration.toHours();
        int minutes = duration.toMinutesPart();
        int seconds = duration.toSecondsPart();
        if (hours > 0) {
            return Messages.format("productivity.text.duration.hours", hours, minutes);
        }
        if (minutes > 0) {
            return Messages.format("productivity.text.duration.minutes", minutes, seconds);
        }
        return Messages.format("productivity.text.duration.seconds", seconds);
    }

    /** Key i18n = prefix + nama enum huruf kecil, contoh {@code productivity.text.tidy.sort_ascending}. */
    private static String labelKey(String prefix, Enum<?> value) {
        return prefix + value.name().toLowerCase(Locale.ROOT);
    }
}
