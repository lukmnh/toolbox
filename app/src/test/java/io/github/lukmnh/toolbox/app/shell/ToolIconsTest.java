package io.github.lukmnh.toolbox.app.shell;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.lukmnh.toolbox.common.tool.Tool;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.IkonResolverProvider;

/** Typo literal ikon di tool hanya muncul sebagai ikon fallback saat runtime; test ini menangkapnya di build. */
class ToolIconsTest {

    @Test
    void everyRegisteredToolHasAResolvableIcon() {
        List<Tool> tools = ToolRegistry.load().tools();
        assertFalse(tools.isEmpty(), "ServiceLoader tidak menemukan tool");

        for (Tool tool : tools) {
            assertResolvable(tool.iconLiteral(), tool.id());
        }
    }

    @Test
    void fallbackIconIsResolvable() {
        assertResolvable(ToolIcon.FALLBACK, "fallback");
    }

    /** Lewat IkonResolver (tanpa membuat FontIcon) supaya test tidak butuh JavaFX toolkit. */
    private static void assertResolvable(String literal, String owner) {
        assertDoesNotThrow(
                // FontLoader no-op: cukup cek literal ter-resolve, font tidak perlu di-load
                () -> IkonResolverProvider.getInstance(handler -> {}).resolve(literal).resolve(literal),
                () -> owner + ": ikon '" + literal + "' tidak ada di Ikonli");
    }
}
