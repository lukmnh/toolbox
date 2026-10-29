package io.github.lukmnh.toolbox.devutils.cron.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronExplanation;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronFieldKind;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronMeaning;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronPreset;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronSchedule;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronTarget;
import io.github.lukmnh.toolbox.devutils.cron.domain.FieldExpr;
import io.github.lukmnh.toolbox.devutils.cron.domain.GoInterval;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DateTimeTexts;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevLayout;
import io.github.lukmnh.toolbox.devutils.shared.presentation.Tone;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Layout Cron: target + zona, Builder, ekspresi + validasi, penjelasan + jadwal berikutnya + snippet, referensi. */
final class CronView {

    private static final double SPINNER_WIDTH = 90;
    private static final double CUSTOM_FIELD_WIDTH = 110;
    private static final double ZONE_WIDTH = 220;
    private static final double SNIPPET_HEIGHT = 70;
    private static final double GRID_GAP = 6;
    private static final int MAX_HOUR = 23;
    private static final int MAX_MINUTE = 59;
    private static final List<CronFieldKind> CUSTOM_LAYOUT = List.of(
            CronFieldKind.MINUTE,
            CronFieldKind.HOUR,
            CronFieldKind.DAY_OF_MONTH,
            CronFieldKind.MONTH,
            CronFieldKind.DAY_OF_WEEK);

    private final CronViewModel viewModel;

    private CronView(CronViewModel viewModel) {
        this.viewModel = viewModel;
    }

    static Node create(CronViewModel viewModel) {
        return new CronView(viewModel).build();
    }

    private Node build() {
        VBox page = DevLayout.page();
        page.getChildren()
                .addAll(
                        DevLayout.header(Messages.get("devutils.cron.name"), Messages.get("devutils.cron.hint")),
                        buildTarget(),
                        buildBuilder(),
                        buildExpression(),
                        buildResult(),
                        buildReference());
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("edge-to-edge");
        return scroll;
    }

    // ---------- Target & zona ----------

    private VBox buildTarget() {
        CheckBox seconds = new CheckBox(Messages.get("devutils.cron.goSeconds"));
        seconds.selectedProperty().bindBidirectional(viewModel.goWithSecondsProperty());
        seconds.setTooltip(new Tooltip(Messages.get("devutils.cron.goSeconds.tooltip")));
        seconds.visibleProperty().bind(viewModel.targetProperty().isEqualTo(CronTarget.GO));
        DevLayout.collapseWhenHidden(seconds);

        ComboBox<String> zone = new ComboBox<>();
        zone.getItems().setAll(ZoneId.getAvailableZoneIds().stream().sorted().toList());
        zone.setEditable(true);
        zone.setPrefWidth(ZONE_WIDTH);
        zone.valueProperty().bindBidirectional(viewModel.zoneProperty());

        Label hint = DevLayout.hint("");
        hint.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> Messages.get("devutils.cron.target.hint."
                                + viewModel.targetProperty().get().name()),
                        viewModel.targetProperty()));

        HBox row = DevLayout.row(
                DevLayout.segmented(viewModel.targetProperty(), List.of(CronTarget.values()), CronTarget::displayName),
                seconds,
                DevLayout.spacer(),
                DevLayout.labeled(Messages.get("devutils.cron.zone"), zone));
        row.setSpacing(DevLayout.SPACING);
        return DevLayout.card(Messages.get("devutils.cron.target"), List.of(), row, hint);
    }

    // ---------- Builder ----------

    private VBox buildBuilder() {
        ChoiceBox<PresetKind> preset = new ChoiceBox<>();
        preset.getItems().setAll(PresetKind.values());
        preset.setConverter(DevLayout.labels(kind -> Messages.get(kind.labelKey())));
        preset.valueProperty().bindBidirectional(viewModel.presetProperty());

        ChoiceBox<DayOfWeek> day = new ChoiceBox<>();
        day.getItems().setAll(DayOfWeek.values());
        day.setConverter(DevLayout.labels(value -> value.getDisplayName(TextStyle.FULL, Messages.LOCALE)));
        day.valueProperty().bindBidirectional(viewModel.dayOfWeekProperty());

        HBox parameters = DevLayout.row(
                shownFor(
                        DevLayout.labeled(
                                Messages.get("devutils.cron.builder.everyMinutes"),
                                spinner(1, MAX_MINUTE, viewModel.everyMinutesProperty())),
                        EnumSet.of(PresetKind.EVERY_MINUTES)),
                shownFor(
                        DevLayout.labeled(Messages.get("devutils.cron.builder.day"), day),
                        EnumSet.of(PresetKind.WEEKLY)),
                shownFor(
                        DevLayout.labeled(
                                Messages.get("devutils.cron.builder.dayOfMonth"),
                                spinner(1, CronPreset.MAX_DAY_OF_MONTH, viewModel.dayOfMonthProperty())),
                        EnumSet.of(PresetKind.MONTHLY)),
                shownFor(
                        DevLayout.labeled(
                                Messages.get("devutils.cron.builder.hour"),
                                spinner(0, MAX_HOUR, viewModel.hourProperty())),
                        EnumSet.of(PresetKind.DAILY, PresetKind.WEEKDAYS, PresetKind.WEEKLY, PresetKind.MONTHLY)),
                shownFor(
                        DevLayout.labeled(
                                Messages.get("devutils.cron.builder.minute"),
                                spinner(0, MAX_MINUTE, viewModel.minuteProperty())),
                        EnumSet.complementOf(EnumSet.of(PresetKind.EVERY_MINUTES, PresetKind.CUSTOM))),
                shownFor(customFields(), EnumSet.of(PresetKind.CUSTOM)));
        parameters.setSpacing(DevLayout.SPACING);

        HBox row = DevLayout.row(DevLayout.labeled(Messages.get("devutils.cron.builder.preset"), preset), parameters);
        row.setSpacing(DevLayout.SPACING);
        return DevLayout.card(
                Messages.get("devutils.cron.builder"),
                List.of(),
                row,
                DevLayout.hint(Messages.get("devutils.cron.builder.hint")));
    }

    private HBox customFields() {
        HBox row = DevLayout.row();
        for (int index = 0; index < CUSTOM_LAYOUT.size(); index++) {
            TextField field = DevLayout.codeField();
            field.setPrefWidth(CUSTOM_FIELD_WIDTH);
            field.textProperty().bindBidirectional(viewModel.customFields().get(index));
            row.getChildren().add(DevLayout.labeled(CronErrorMessages.fieldName(CUSTOM_LAYOUT.get(index)), field));
        }
        return row;
    }

    private Node shownFor(Node node, Set<PresetKind> presets) {
        node.visibleProperty()
                .bind(Bindings.createBooleanBinding(
                        () -> presets.contains(viewModel.presetProperty().get()), viewModel.presetProperty()));
        DevLayout.collapseWhenHidden(node);
        return node;
    }

    private static Spinner<Integer> spinner(int min, int max, ObjectProperty<Integer> value) {
        Spinner<Integer> spinner = new Spinner<>(min, max, value.get());
        spinner.setEditable(true);
        spinner.setPrefWidth(SPINNER_WIDTH);
        spinner.getValueFactory().valueProperty().bindBidirectional(value);
        return spinner;
    }

    // ---------- Ekspresi ----------

    private VBox buildExpression() {
        TextField expression = DevLayout.codeField();
        expression.setPromptText(Messages.get("devutils.cron.expression.prompt"));
        expression.textProperty().bindBidirectional(viewModel.expressionProperty());

        // Listener langsung ke property ViewModel: binding turunan (isNotNull) tanpa referensi kuat bisa terkena GC
        Label badge = DevLayout.badge("", Tone.NEUTRAL);
        viewModel
                .explanationProperty()
                .addListener((observable, previous, current) -> updateBadge(badge, current != null));
        updateBadge(badge, viewModel.explanationProperty().get() != null);

        Button copy = DevLayout.button(Messages.get("devutils.copy"), viewModel::copyExpression);
        Label error = DevLayout.inlineMessage(Tone.DANGER);
        error.textProperty().bind(viewModel.errorProperty());

        GridPane fields = new GridPane();
        fields.setHgap(DevLayout.SPACING);
        fields.setVgap(GRID_GAP);
        viewModel.explanationProperty().addListener((observable, previous, current) -> fillFields(fields, current));
        fillFields(fields, viewModel.explanationProperty().get());

        HBox row = DevLayout.row(expression, badge, copy);
        HBox.setHgrow(expression, Priority.ALWAYS);
        return DevLayout.card(Messages.get("devutils.cron.expression"), List.of(), row, error, fields);
    }

    private static void updateBadge(Label badge, boolean valid) {
        DevLayout.update(
                badge,
                Messages.get(valid ? "devutils.cron.valid" : "devutils.cron.invalid"),
                valid ? Tone.SUCCESS : Tone.DANGER);
    }

    /** Rincian arti per field; untuk {@code @every} cukup satu baris interval. */
    private static void fillFields(GridPane grid, CronExplanation explanation) {
        grid.getChildren().clear();
        if (explanation == null || !(explanation.meaning() instanceof CronSchedule schedule)) {
            return;
        }
        int row = 0;
        for (CronFieldKind kind : CronFieldKind.values()) {
            if (!schedule.fields().containsKey(kind)) {
                continue;
            }
            Label name = new Label(CronErrorMessages.fieldName(kind));
            name.getStyleClass().add("field-label");
            Label meaning = new Label(CronDescriber.field(kind, schedule.field(kind)));
            meaning.setWrapText(true);
            grid.addRow(row++, name, meaning);
        }
    }

    // ---------- Hasil ----------

    private VBox buildResult() {
        Label description = new Label();
        description.getStyleClass().add("card-title");
        description.setWrapText(true);
        description
                .textProperty()
                .bind(Bindings.createStringBinding(
                        () -> describe(viewModel.explanationProperty().get()), viewModel.explanationProperty()));

        VBox runs = new VBox(4);
        Label noRuns = DevLayout.inlineMessage(Tone.WARNING);
        Label zoneNote = DevLayout.hint("");
        viewModel
                .explanationProperty()
                .addListener((observable, previous, current) -> fillRuns(runs, noRuns, zoneNote, current));

        TextArea snippet = DevLayout.codeArea(false);
        snippet.setPrefHeight(SNIPPET_HEIGHT);
        snippet.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> {
                            CronExplanation current =
                                    viewModel.explanationProperty().get();
                            return current == null ? "" : current.snippet();
                        },
                        viewModel.explanationProperty()));
        Button copySnippet = DevLayout.button(Messages.get("devutils.copy"), viewModel::copySnippet);

        VBox card = DevLayout.card(
                Messages.get("devutils.cron.result"),
                List.of(),
                description,
                DevLayout.labeled(Messages.get("devutils.cron.nextRuns"), runs),
                noRuns,
                zoneNote,
                DevLayout.card(Messages.get("devutils.cron.snippet"), List.of(copySnippet), snippet));
        card.visibleProperty().bind(viewModel.explanationProperty().isNotNull());
        DevLayout.collapseWhenHidden(card);
        return card;
    }

    private static String describe(CronExplanation explanation) {
        return explanation == null ? "" : CronDescriber.describe(explanation.meaning());
    }

    private static void fillRuns(VBox runs, Label noRuns, Label zoneNote, CronExplanation explanation) {
        runs.getChildren().clear();
        noRuns.setText("");
        zoneNote.setText("");
        if (explanation == null) {
            return;
        }
        DateTimeFormatter format =
                showsSeconds(explanation.meaning()) ? DateTimeTexts.HUMAN_WITH_SECONDS : DateTimeTexts.HUMAN;
        int number = 1;
        for (ZonedDateTime run : explanation.nextRuns()) {
            Label label = new Label(number++ + ".  " + format.format(run));
            label.getStyleClass().add("stat-text");
            runs.getChildren().add(label);
        }
        if (explanation.nextRuns().isEmpty()) {
            noRuns.setText(Messages.get("devutils.cron.noRuns"));
        }
        zoneNote.setText(zoneNote(explanation));
    }

    private static boolean showsSeconds(CronMeaning meaning) {
        return switch (meaning) {
            case CronSchedule schedule ->
                schedule.hasSeconds() && !schedule.field(CronFieldKind.SECOND).equals(new FieldExpr.Value(0));
            case GoInterval interval -> interval.delay().toSecondsPart() != 0;
        };
    }

    private static String zoneNote(CronExplanation explanation) {
        return switch (explanation.target()) {
            case UNIX -> Messages.get("devutils.cron.zoneNote.unix");
            case GO ->
                explanation.meaning() instanceof GoInterval
                        ? Messages.get("devutils.cron.zoneNote.goInterval")
                        : Messages.format(
                                "devutils.cron.zoneNote.schedule",
                                explanation.scheduleZone().getId());
            default ->
                Messages.format(
                        "devutils.cron.zoneNote.schedule",
                        explanation.scheduleZone().getId());
        };
    }

    // ---------- Referensi ----------

    private TitledPane buildReference() {
        Label table = new Label();
        table.getStyleClass().add("code-field");
        table.setWrapText(true);
        table.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> Messages.get("devutils.cron.reference."
                                + viewModel.targetProperty().get().name()),
                        viewModel.targetProperty()));
        VBox content = new VBox(DevLayout.GAP, table, DevLayout.hint(Messages.get("devutils.cron.reference.common")));
        content.setPadding(new Insets(DevLayout.GAP));
        TitledPane pane = new TitledPane(Messages.get("devutils.cron.reference"), content);
        pane.setExpanded(false);
        return pane;
    }
}
