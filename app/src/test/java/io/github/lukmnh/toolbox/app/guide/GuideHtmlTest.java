package io.github.lukmnh.toolbox.app.guide;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.settings.Theme;
import org.junit.jupiter.api.Test;

class GuideHtmlTest {

    private final GuideHtml html = new GuideHtml();

    @Test
    void rendersMarkdown() {
        String body = html.body("# Title\n\n- **bold** item");

        assertTrue(body.contains("<h1>Title</h1>"), body);
        assertTrue(body.contains("<li><strong>bold</strong> item</li>"), body);
    }

    @Test
    void rawHtmlIsEscaped() {
        String body = html.body("<script>alert(1)</script>\n\nText <img src=x onerror=alert(1)>");

        assertFalse(body.contains("<script"), body);
        assertFalse(body.contains("<img"), body);
        assertTrue(body.contains("&lt;script&gt;"), body);
    }

    @Test
    void dangerousUrlsAreSanitized() {
        String body = html.body("[click](javascript:alert(1))");

        assertFalse(body.contains("javascript:"), body);
    }

    @Test
    void documentBlocksScriptsAndFollowsTheme() {
        String light = html.document("Hi", Theme.LIGHT);
        String dark = html.document("Hi", Theme.DARK);

        assertTrue(light.contains("Content-Security-Policy"), light);
        assertTrue(light.contains("default-src 'none'"), light);
        assertTrue(light.contains("color-scheme: light"), light);
        assertTrue(dark.contains("color-scheme: dark"), dark);
    }
}
