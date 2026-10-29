package io.github.lukmnh.toolbox.devutils.regex.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.devutils.regex.application.ReplaceResult;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevLayout;
import io.github.lukmnh.toolbox.devutils.shared.presentation.Tone;
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
        TextField template = DevLayout.codeField();
        template.setPromptText(Messages.get("devutils.regex.replace.prompt"));
        template.textProperty().bindBidirectional(viewModel.replacementProperty());
        Label hint = DevLayout.hint("");
        hint.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> Messages.get("devutils.regex.replace.hint."
                                + viewModel.flavorProperty().get().name()),
                        viewModel.flavorProperty()));
        Label error = DevLayout.inlineMessage(Tone.DANGER);
        error.textProperty().bind(viewModel.replaceErrorProperty());

        TextArea output = DevLayout.codeArea(false);
        VBox.setVgrow(output, Priority.ALWAYS);
        Label count = DevLayout.statText();
        viewModel.evaluationProperty().addListener((observable, previous, current) -> {
            ReplaceResult result = current == null ? null : current.replace().orElse(null);
            output.setText(result == null ? "" : result.output());
            count.setText(
                    result == null
                            ? ""
                            : Messages.format(
                                    "devutils.regex.replace.count", RegexMessages.number(result.replacements())));
        });
        Button copy = DevLayout.button(Messages.get("devutils.copy"), viewModel::copyReplaceOutput);
        copy.disableProperty().bind(output.textProperty().isEmpty());

        VBox box = new VBox(
                DevLayout.GAP,
                DevLayout.labeled(Messages.get("devutils.regex.replace.template"), template),
                hint,
                error,
                DevLayout.row(
                        new Label(Messages.get("devutils.regex.replace.output")), DevLayout.spacer(), count, copy),
                output);
        box.setPadding(new Insets(DevLayout.GAP));
        return box;
    }
}
