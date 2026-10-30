package io.github.lukmnh.toolbox.common.ui;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import java.util.Map;
import java.util.function.Supplier;
import javafx.scene.control.Button;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;

/** Copy teks hasil ke clipboard sistem. Harus dipanggil dari FX thread. */
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
}
