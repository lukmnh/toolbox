package io.github.lukmnh.toolbox.app.shell;

import io.github.lukmnh.toolbox.common.tool.Tool;
import java.lang.System.Logger.Level;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;

/** Ikon tool: kotak berwarna aksen kategori berisi glyph Remix Icon. Dipakai Home, sidebar, dan header. */
final class ToolIcon {

    private static final System.Logger LOG = System.getLogger(ToolIcon.class.getName());
    static final String FALLBACK = "remixmz-tools-line";

    private ToolIcon() {}

    /** Literal tidak dikenal (typo di tool) tidak boleh menggagalkan Home; dicatat lalu pakai ikon default. */
    static FontIcon glyph(String literal) {
        try {
            return new FontIcon(literal);
        } catch (IllegalArgumentException e) {
            LOG.log(Level.WARNING, "Ikon tidak dikenal: " + literal, e);
            return new FontIcon(FALLBACK);
        }
    }

    /** @param variant style class ukuran: {@code tool-icon} (kartu), {@code tool-icon-small} (pill/sidebar) */
    static StackPane tile(Tool tool, String variant) {
        StackPane tile = new StackPane(glyph(tool.iconLiteral()));
        tile.getStyleClass().addAll(variant, tool.category().styleClass());
        return tile;
    }
}
