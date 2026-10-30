package io.github.lukmnh.toolbox.devutils.regex.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.devutils.regex.application.ReplaceResult;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Tab Replace: template dengan sintaks replacement flavor aktif, lalu preview hasil. */
final class ReplacePane {

    private final RegexViewModel viewModel;

    private ReplacePane(RegexViewModel viewModel) {
        this.viewModel = viewModel;
    }

    static Node create(RegexViewModel viewModel) {
        return new ReplacePane(viewModel).build();
    }

    private Node build() {
        TextField template = ToolLayout.codeField();
        template.setPromptText(Messages.get("devutils.regex.replace.prompt"));
        template.textProperty().bindBidirectional(viewModel.replacementProperty());
        Label hint = ToolLayout.hint("");
        hint.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> Messages.get("devutils.regex.replace.hint."
                                + viewModel.flavorProperty().get().name()),
                        viewModel.flavorProperty()));
        Label error = ToolLayout.inlineMessage(Tone.DANGER);
        error.textProperty().bind(viewModel.replaceErrorProperty());

        TextArea output = ToolLayout.codeArea(false);
        VBox.setVgrow(output, Priority.ALWAYS);
        Label count = ToolLayout.statText();
        viewModel.evaluationProperty().addListener((observable, previous, current) -> {
            ReplaceResult result = current == null ? null : current.replace().orElse(null);
            output.setText(result == null ? "" : result.output());
            count.setText(
                    result == null
                            ? ""
                            : Messages.format(
                                    "devutils.regex.replace.count", RegexMessages.number(result.replacements())));
        });
        Button copy = ToolLayout.button(Messages.get("common.copy"), viewModel::copyReplaceOutput);
        copy.disableProperty().bind(output.textProperty().isEmpty());

        VBox box = new VBox(
                ToolLayout.GAP,
                ToolLayout.labeled(Messages.get("devutils.regex.replace.template"), template),
                hint,
                error,
                ToolLayout.row(
                        new Label(Messages.get("devutils.regex.replace.output")), ToolLayout.spacer(), count, copy),
                output);
        box.setPadding(new Insets(ToolLayout.GAP));
        return box;
    }
}
