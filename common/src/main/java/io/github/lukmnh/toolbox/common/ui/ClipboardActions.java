package io.github.lukmnh.toolbox.common.ui;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import java.util.Map;
import java.util.function.Supplier;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;

/** Copy hasil dan "Bersihkan clipboard" (Security Requirements #8). Harus dipanggil dari FX thread. */
public final class ClipboardActions {

    private ClipboardActions() {}

    public static void copy(String text, Notifier notifier) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.PLAIN_TEXT, text));
        notifier.info(Messages.get("common.copied"));
    }

    public static Button copyButton(Supplier<String> text, Notifier notifier) {
        Button button = new Button(Messages.get("common.copy"));
        button.setOnAction(event -> copy(text.get(), notifier));
        return button;
    }

    /** Hasil sensitif (hash, token, Base64) bisa tertinggal di clipboard setelah tool ditutup. */
    public static Button clearButton(Notifier notifier) {
        Button button = new Button(Messages.get("common.clipboard.clear"));
        button.setTooltip(new Tooltip(Messages.get("common.clipboard.clear.tooltip")));
        button.setOnAction(event -> {
            Clipboard.getSystemClipboard().clear();
            notifier.info(Messages.get("common.clipboard.cleared"));
        });
        return button;
    }
}
