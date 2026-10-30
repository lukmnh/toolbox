package io.github.lukmnh.toolbox.devutils.git.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.ui.ClipboardActions;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.devutils.git.domain.GitArgument;
import io.github.lukmnh.toolbox.devutils.git.domain.GitBuild;
import io.github.lukmnh.toolbox.devutils.git.domain.GitCatalog;
import io.github.lukmnh.toolbox.devutils.git.domain.GitCheatsheet;
import io.github.lukmnh.toolbox.devutils.git.domain.GitCommandSpec;
import io.github.lukmnh.toolbox.devutils.git.domain.GitIssue;
import io.github.lukmnh.toolbox.devutils.git.domain.GitOption;
import io.github.lukmnh.toolbox.devutils.git.domain.GitPart;
import io.github.lukmnh.toolbox.devutils.git.domain.GitSnippet;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Layout Git: mode Builder (command → form → hasil + penjelasan) dan Cheatsheet (syntax per grup + search). */
final class GitView {

    private static final double FIELD_WIDTH = 220;
    private static final double TOKEN_WIDTH = 220;
    private static final double SNIPPET_WIDTH = 340;
    private static final int VISIBLE_COMMANDS = 12;

    private enum Mode {
        BUILDER("devutils.git.mode.builder"),
        CHEATSHEET("devutils.git.mode.cheatsheet");

        private final String messageKey;

        Mode(String messageKey) {
            this.messageKey = messageKey;
        }
    }

    private final GitViewModel viewModel;
    private final VBox form = new VBox(ToolLayout.SPACING);
    private final VBox explanation = new VBox(ToolLayout.GAP);
    private final Label issues = ToolLayout.inlineMessage(Tone.WARNING);
    private final VBox groups = new VBox(ToolLayout.SPACING);

    private GitView(GitViewModel viewModel) {
        this.viewModel = viewModel;
    }

    static Node create(GitViewModel viewModel) {
        return new GitView(viewModel).build();
    }

    private Node build() {
        ObjectProperty<Mode> mode = new SimpleObjectProperty<>(Mode.BUILDER);
        Node builder = buildBuilder();
        Node cheatsheet = buildCheatsheet();
        StackPane content = new StackPane(builder);
        mode.addListener((observable, previous, current) ->
                content.getChildren().setAll(current == Mode.BUILDER ? builder : cheatsheet));

        VBox page = ToolLayout.page();
        page.getChildren()
                .addAll(
                        ToolLayout.header(
                                ToolCategory.DEV_UTILS,
                                Messages.get("devutils.git.name"),
                                Messages.get("devutils.git.hint")),
                        ToolLayout.segmented(mode, List.of(Mode.values()), option -> Messages.get(option.messageKey)),
                        content);
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("edge-to-edge");
        return scroll;
    }

    // ---------- Builder ----------

    private VBox buildBuilder() {
        ComboBox<GitCommandSpec> command = new ComboBox<>();
        command.getItems().setAll(GitCatalog.COMMANDS);
        command.setConverter(ToolLayout.labels(spec -> "git " + spec.verb()));
        command.setVisibleRowCount(VISIBLE_COMMANDS);
        command.setPrefWidth(FIELD_WIDTH);
        command.valueProperty().bindBidirectional(viewModel.commandProperty());
        Label description = ToolLayout.hint("");
        description.textProperty().bind(viewModel.commandProperty().map(GitTexts::command));

        // Listener view didaftarkan setelah listener view model, jadi field command baru sudah tersedia
        viewModel.commandProperty().addListener((observable, previous, current) -> renderForm(current));
        viewModel.resultProperty().addListener((observable, previous, current) -> renderResult(current));
        renderForm(viewModel.commandProperty().get());

        TextField output = ToolLayout.codeField();
        output.setEditable(false);
        output.textProperty().bind(viewModel.resultProperty().map(GitBuild::command));
        renderResult(viewModel.resultProperty().get());

        VBox commandCard = ToolLayout.card(
                Messages.get("devutils.git.command"),
                List.of(),
                ToolLayout.labeled(Messages.get("devutils.git.command"), command),
                description,
                form);
        VBox resultCard = ToolLayout.card(
                Messages.get("devutils.git.result"),
                List.of(ClipboardActions.copyButton(output::getText, viewModel.notifier())),
                output,
                issues,
                ToolLayout.labeled(Messages.get("devutils.git.explanation"), explanation));
        return new VBox(ToolLayout.SPACING, commandCard, resultCard);
    }

    /** Field argumen dan opsi untuk command terpilih; dibangun ulang setiap ganti command. */
    private void renderForm(GitCommandSpec spec) {
        form.getChildren().clear();
        FlowPane fields = new FlowPane(ToolLayout.SPACING, ToolLayout.SPACING);
        for (GitArgument argument : spec.arguments()) {
            String label = GitTexts.argument(spec, argument.key());
            fields.getChildren()
                    .add(ToolLayout.labeled(
                            argument.required() ? label + " *" : label,
                            textField(label, viewModel.argument(argument.key()))));
        }
        FlowPane checks = new FlowPane(ToolLayout.SPACING, ToolLayout.GAP);
        for (GitOption option : spec.options()) {
            String explanationText = GitTexts.option(spec, option.key());
            if (option.takesValue()) {
                fields.getChildren()
                        .add(ToolLayout.labeled(
                                option.flag(), textField(explanationText, viewModel.value(option.key()))));
            } else {
                CheckBox check = ToolLayout.mono(new CheckBox(option.flag()));
                check.selectedProperty().bindBidirectional(viewModel.flag(option.key()));
                check.setTooltip(new Tooltip(explanationText));
                checks.getChildren().add(check);
            }
        }
        if (!fields.getChildren().isEmpty()) {
            form.getChildren().add(fields);
        }
        if (!checks.getChildren().isEmpty()) {
            form.getChildren().add(ToolLayout.labeled(Messages.get("devutils.git.options"), checks));
        }
    }

    private static TextField textField(String prompt, StringProperty value) {
        TextField field = ToolLayout.mono(new TextField());
        field.setPromptText(prompt);
        field.setPrefWidth(FIELD_WIDTH);
        field.setTooltip(new Tooltip(prompt));
        field.textProperty().bindBidirectional(value);
        return field;
    }

    /** Penjelasan per token + peringatan input (argumen wajib kosong, opsi bentrok). */
    private void renderResult(GitBuild build) {
        GitCommandSpec spec = viewModel.commandProperty().get();
        explanation.getChildren().clear();
        for (GitPart part : build.parts()) {
            Label token = ToolLayout.mono(new Label(part.text()));
            token.setMinWidth(TOKEN_WIDTH);
            token.setPrefWidth(TOKEN_WIDTH);
            Label text = new Label(GitTexts.part(spec, part));
            text.setWrapText(true);
            HBox.setHgrow(text, Priority.ALWAYS);
            HBox row = ToolLayout.row(token, text);
            if (part.dangerous()) {
                row.getChildren().add(ToolLayout.badge(Messages.get("devutils.git.dangerous"), Tone.WARNING));
            }
            explanation.getChildren().add(row);
        }
        issues.setText(
                build.issues().stream().map(issue -> describe(spec, issue)).collect(Collectors.joining("\n")));
    }

    private static String describe(GitCommandSpec spec, GitIssue issue) {
        return switch (issue.kind()) {
            case MISSING_ARGUMENT ->
                Messages.format(
                        "devutils.git.issue.missing",
                        GitTexts.argument(spec, issue.keys().getFirst()));
            case CONFLICT -> Messages.format("devutils.git.issue.conflict", flags(spec, issue.keys()));
        };
    }

    private static String flags(GitCommandSpec spec, List<String> keys) {
        return spec.options().stream()
                .filter(option -> keys.contains(option.key()))
                .map(GitOption::flag)
                .collect(Collectors.joining(", "));
    }

    // ---------- Cheatsheet ----------

    private VBox buildCheatsheet() {
        TextField search = new TextField();
        search.setPromptText(Messages.get("devutils.git.search"));
        search.textProperty().bindBidirectional(viewModel.queryProperty());
        viewModel.queryProperty().addListener((observable, previous, current) -> renderCheatsheet());
        renderCheatsheet();
        return new VBox(ToolLayout.SPACING, search, groups);
    }

    private void renderCheatsheet() {
        groups.getChildren().clear();
        List<GitSnippet> matches = viewModel.snippets();
        for (String group : GitCheatsheet.GROUPS) {
            List<GitSnippet> inGroup = matches.stream()
                    .filter(snippet -> snippet.group().equals(group))
                    .toList();
            if (inGroup.isEmpty()) {
                continue;
            }
            VBox rows = new VBox(ToolLayout.GAP);
            inGroup.forEach(snippet -> rows.getChildren().add(snippetRow(snippet)));
            groups.getChildren().add(ToolLayout.card(GitTexts.group(group), List.of(), rows));
        }
        if (groups.getChildren().isEmpty()) {
            groups.getChildren().add(ToolLayout.hint(Messages.get("devutils.git.noResult")));
        }
    }

    private HBox snippetRow(GitSnippet snippet) {
        Label command = ToolLayout.mono(new Label(snippet.command()));
        command.setMinWidth(SNIPPET_WIDTH);
        command.setPrefWidth(SNIPPET_WIDTH);
        Label text = new Label(GitTexts.snippet(snippet));
        text.setWrapText(true);
        HBox.setHgrow(text, Priority.ALWAYS);
        return ToolLayout.row(command, text, ClipboardActions.copyButton(snippet::command, viewModel.notifier()));
    }
}
