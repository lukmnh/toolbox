package io.github.lukmnh.toolbox.devutils.jwt.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.Notifier;
import io.github.lukmnh.toolbox.devutils.shared.presentation.ClipboardActions;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevLayout;
import io.github.lukmnh.toolbox.devutils.shared.presentation.Tone;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Layout JWT Debugger: token di atas, baris badge status, lalu dua kolom: isi token (header, payload, signature) di
 * kiri, claims dan verifikasi di kanan.
 */
public final class JwtView extends ScrollPane {

    private static final int TOKEN_ROWS = 4;
    private static final int HEADER_ROWS = 6;
    private static final int PAYLOAD_ROWS = 10;
    private static final int KEY_ROWS = 6;
    private static final double CLAIMS_HEIGHT = 300;
    private static final int MAX_SKEW_SECONDS = 86_400;
    private static final int SKEW_STEP_SECONDS = 30;
    private static final double SKEW_SPINNER_WIDTH = 110;

    private final JwtViewModel viewModel;
    private final Notifier notifier;

    public JwtView(JwtViewModel viewModel, Notifier notifier) {
        this.viewModel = viewModel;
        this.notifier = notifier;
        VBox page = DevLayout.page();
        page.getChildren()
                .addAll(
                        DevLayout.header(Messages.get("devutils.jwt.name"), Messages.get("devutils.jwt.hint")),
                        buildTokenCard(),
                        buildBadgeRow(),
                        buildColumns());
        setContent(page);
        setFitToWidth(true);
        getStyleClass().add("tool-view");
    }

    private VBox buildTokenCard() {
        TextArea token = DevLayout.codeArea(true);
        token.setPromptText(Messages.get("devutils.jwt.token.prompt"));
        token.setPrefRowCount(TOKEN_ROWS);
        token.textProperty().bindBidirectional(viewModel.tokenProperty());
        Label error = DevLayout.inlineMessage(Tone.DANGER);
        error.textProperty().bind(viewModel.decodeErrorProperty());
        Label warning = DevLayout.inlineMessage(Tone.WARNING);
        warning.textProperty().bind(viewModel.decodeWarningProperty());
        Button clear = DevLayout.button(
                Messages.get("devutils.jwt.token.clear"),
                () -> viewModel.tokenProperty().set(""));
        return DevLayout.card(
                Messages.get("devutils.jwt.token.title"),
                List.of(clear, ClipboardActions.clearButton(notifier)),
                token,
                DevLayout.hint(Messages.get("devutils.jwt.token.hint")),
                error,
                warning);
    }

    private HBox buildBadgeRow() {
        HBox row = DevLayout.row(
                caption("devutils.jwt.badge.format"),
                badge(viewModel.formatStatusProperty()),
                caption("devutils.jwt.badge.time"),
                badge(viewModel.timeStatusProperty()),
                caption("devutils.jwt.badge.signature"),
                badge(viewModel.signatureStatusProperty()),
                DevLayout.spacer(),
                badge(viewModel.algorithmStatusProperty()));
        row.setSpacing(DevLayout.SPACING);
        return row;
    }

    private HBox buildColumns() {
        VBox left = new VBox(DevLayout.SPACING, buildHeaderCard(), buildPayloadCard(), buildSignatureCard());
        VBox right = new VBox(DevLayout.SPACING, buildClaimsCard(), buildVerifyCard());
        // prefWidth 0 + hgrow membuat dua kolom selalu sama lebar
        for (VBox column : List.of(left, right)) {
            column.setPrefWidth(0);
            HBox.setHgrow(column, Priority.ALWAYS);
        }
        return new HBox(DevLayout.SPACING * 1.5, left, right);
    }

    private VBox buildHeaderCard() {
        return codeCard("devutils.jwt.header.title", viewModel.headerProperty(), HEADER_ROWS);
    }

    private VBox buildPayloadCard() {
        return codeCard("devutils.jwt.payload.title", viewModel.payloadProperty(), PAYLOAD_ROWS);
    }

    private VBox codeCard(String titleKey, ObservableValue<String> text, int rows) {
        TextArea area = DevLayout.codeArea(false);
        area.setPrefRowCount(rows);
        area.textProperty().bind(text);
        return DevLayout.card(
                Messages.get(titleKey), List.of(ClipboardActions.copyButton(text::getValue, notifier)), area);
    }

    private VBox buildSignatureCard() {
        TextField signature = DevLayout.codeField();
        signature.setEditable(false);
        signature.textProperty().bind(viewModel.signatureProperty());
        return DevLayout.card(
                Messages.get("devutils.jwt.signature.title"),
                List.of(ClipboardActions.copyButton(viewModel.signatureProperty()::get, notifier)),
                signature,
                DevLayout.hint(Messages.get("devutils.jwt.signature.hint")));
    }

    private VBox buildClaimsCard() {
        TableView<ClaimRow> table = new TableView<>(viewModel.claims());
        table.getColumns()
                .setAll(List.of(
                        column("devutils.jwt.claims.column.name", ClaimRow::name),
                        column("devutils.jwt.claims.column.value", ClaimRow::value),
                        column("devutils.jwt.claims.column.explanation", ClaimRow::explanation)));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label(Messages.get("devutils.jwt.claims.empty")));
        table.setPrefHeight(CLAIMS_HEIGHT);
        return DevLayout.card(
                Messages.get("devutils.jwt.claims.title"),
                List.of(),
                table,
                DevLayout.hint(Messages.get("devutils.jwt.claims.hint")));
    }

    private static TableColumn<ClaimRow, String> column(String titleKey, Function<ClaimRow, String> value) {
        TableColumn<ClaimRow, String> column = new TableColumn<>(Messages.get(titleKey));
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(value.apply(data.getValue())));
        column.setSortable(false);
        return column;
    }

    private VBox buildVerifyCard() {
        Label hint = DevLayout.hint("");
        hint.textProperty().bind(viewModel.verifyHint());

        PasswordField secret = new PasswordField();
        secret.setPromptText(Messages.get("devutils.jwt.verify.secret.prompt"));
        CheckBox base64 = new CheckBox(Messages.get("devutils.jwt.verify.secret.base64"));
        base64.selectedProperty().bindBidirectional(viewModel.secretBase64Property());
        VBox secretBox = new VBox(DevLayout.GAP, secret, base64);
        showOnlyIn(secretBox, KeyMode.SECRET);

        TextArea publicKey = DevLayout.codeArea(true);
        publicKey.setPromptText(Messages.get("devutils.jwt.verify.publicKey.prompt"));
        publicKey.setPrefRowCount(KEY_ROWS);
        publicKey.textProperty().bindBidirectional(viewModel.publicKeyTextProperty());
        showOnlyIn(publicKey, KeyMode.PUBLIC_KEY);

        Button verify = DevLayout.accentButton(Messages.get("devutils.jwt.verify.button"), () -> verify(secret));
        verify.disableProperty().bind(viewModel.canVerify().not());
        Label result = DevLayout.inlineMessage(Tone.INFO);
        bindInline(result, viewModel.verifyResultProperty());

        return DevLayout.card(
                Messages.get("devutils.jwt.verify.title"),
                List.of(),
                hint,
                secretBox,
                publicKey,
                DevLayout.row(
                        DevLayout.labeled(Messages.get("devutils.jwt.verify.skew"), skewSpinner()),
                        DevLayout.spacer(),
                        verify),
                result);
    }

    private Spinner<Integer> skewSpinner() {
        Spinner<Integer> spinner = new Spinner<>(
                0, MAX_SKEW_SECONDS, viewModel.clockSkewSecondsProperty().get(), SKEW_STEP_SECONDS);
        spinner.setEditable(true);
        spinner.setPrefWidth(SKEW_SPINNER_WIDTH);
        spinner.getValueFactory().valueProperty().bindBidirectional(viewModel.clockSkewSecondsProperty());
        return spinner;
    }

    /** Secret diambil sebagai {@code char[]} lalu field langsung dikosongkan (Security #7). */
    private void verify(PasswordField secret) {
        if (viewModel.keyModeProperty().get() == KeyMode.SECRET) {
            char[] chars = toChars(secret.getCharacters());
            secret.clear();
            viewModel.verifySecret(chars);
            return;
        }
        viewModel.verifyPublicKey();
    }

    private void showOnlyIn(Node node, KeyMode mode) {
        node.visibleProperty().bind(viewModel.keyModeProperty().isEqualTo(mode));
        DevLayout.collapseWhenHidden(node);
    }

    private static Label caption(String key) {
        Label label = new Label(Messages.get(key));
        label.getStyleClass().add("field-label");
        return label;
    }

    private static Label badge(ReadOnlyObjectProperty<StatusText> state) {
        Label badge = DevLayout.badge(state.get().text(), state.get().tone());
        state.addListener((observable, previous, current) -> DevLayout.update(badge, current.text(), current.tone()));
        return badge;
    }

    private static void bindInline(Label label, ReadOnlyObjectProperty<StatusText> state) {
        DevLayout.update(label, state.get().text(), state.get().tone());
        state.addListener((observable, previous, current) -> DevLayout.update(label, current.text(), current.tone()));
    }

    /** Disalin per karakter supaya tidak membuat {@link String} yang tidak bisa di-wipe. */
    private static char[] toChars(CharSequence typed) {
        char[] chars = new char[typed.length()];
        for (int index = 0; index < chars.length; index++) {
            chars[index] = typed.charAt(index);
        }
        return chars;
    }
}
