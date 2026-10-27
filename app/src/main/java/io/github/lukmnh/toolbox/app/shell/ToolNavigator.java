package io.github.lukmnh.toolbox.app.shell;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.scene.Node;

/**
 * Menjaga aturan "hanya satu tool aktif": sebelum pindah, cek {@code canClose()} (konfirmasi bila perlu),
 * {@code dispose()} tool lama, buang view-nya, lalu buat view tool baru secara lazy.
 */
final class ToolNavigator {

    private final ToolContext context;
    private final Consumer<Node> showContent;
    private Tool active;

    ToolNavigator(ToolContext context, Consumer<Node> showContent) {
        this.context = context;
        this.showContent = showContent;
    }

    Optional<Tool> active() {
        return Optional.ofNullable(active);
    }

    /** @return {@code false} bila user membatalkan karena ada perubahan yang belum disimpan */
    boolean open(Tool tool) {
        if (tool == active) {
            return true;
        }
        if (!closeActive()) {
            return false;
        }
        showContent.accept(tool.createView(context));
        active = tool;
        return true;
    }

    boolean closeActive() {
        if (active == null) {
            return true;
        }
        if (!active.canClose() && !confirmDiscard()) {
            return false;
        }
        disposeActive();
        return true;
    }

    void disposeActive() {
        if (active != null) {
            Tool closing = active;
            active = null;
            closing.dispose();
        }
    }

    private boolean confirmDiscard() {
        return ErrorDialogs.confirm(
                context.owner(),
                Messages.get("confirm.discard.header"),
                Messages.get("confirm.discard.detail"),
                Messages.get("confirm.discard.button"));
    }
}
