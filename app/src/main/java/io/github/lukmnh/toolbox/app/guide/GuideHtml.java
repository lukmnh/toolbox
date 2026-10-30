package io.github.lukmnh.toolbox.app.guide;

import io.github.lukmnh.toolbox.common.settings.Theme;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Markdown → dokumen HTML lengkap untuk preview di WebView.
 *
 * <p>Isi guide bisa diedit user, jadi HTML mentah di Markdown di-escape dan URL berbahaya ({@code javascript:} dsb.)
 * dinetralkan. CSP memblokir script dan resource eksternal (aplikasi offline, tanpa request jaringan).
 */
public final class GuideHtml {

    private static final String CONTENT_SECURITY_POLICY =
            "default-src 'none'; style-src 'unsafe-inline'; img-src data:";

    /** Gaya Swiss minimal: sans-serif sistem, hitam-putih, line height lega, lebar baca ~72 karakter. */
    private static final String STYLE = """
            :root { color-scheme: %s; }
            html { background: %s; }
            body {
              margin: 0 auto; padding: 32px 40px 64px; max-width: 72ch;
              font-family: system-ui, -apple-system, "Segoe UI", "Helvetica Neue", Helvetica, Arial, sans-serif;
              font-size: 15px; line-height: 1.7; color: %s; background: %s;
            }
            h1, h2, h3, h4 { line-height: 1.2; font-weight: 700; letter-spacing: -0.01em; margin: 2em 0 0.6em; }
            h1 { font-size: 2em; margin-top: 0; padding-bottom: 0.4em; border-bottom: 2px solid %s; }
            h2 { font-size: 1.35em; padding-bottom: 0.3em; border-bottom: 1px solid %s; }
            h3 { font-size: 1.1em; }
            p, ul, ol, pre, blockquote { margin: 0 0 1em; }
            ul, ol { padding-left: 1.4em; }
            li { margin: 0.25em 0; }
            a { color: inherit; text-decoration: underline; text-underline-offset: 2px; }
            code, pre { font-family: ui-monospace, "SF Mono", Menlo, Consolas, "Liberation Mono", monospace; }
            code { font-size: 0.9em; padding: 0.1em 0.3em; background: %s; }
            pre { padding: 12px 16px; overflow-x: auto; background: %s; }
            pre code { padding: 0; background: none; }
            kbd { font-family: inherit; font-size: 0.85em; padding: 0.05em 0.4em; border: 1px solid %s; }
            blockquote { padding-left: 1em; border-left: 2px solid %s; color: %s; }
            hr { border: 0; border-top: 1px solid %s; margin: 2em 0; }
            """;

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer =
            HtmlRenderer.builder().escapeHtml(true).sanitizeUrls(true).build();

    /** Fragment HTML (tanpa {@code <html>}), dipisah supaya escaping bisa dites tanpa stylesheet. */
    public String body(String markdown) {
        return renderer.render(parser.parse(markdown));
    }

    public String document(String markdown, Theme theme) {
        return """
                <!DOCTYPE html>
                <html><head><meta charset="utf-8">
                <meta http-equiv="Content-Security-Policy" content="%s">
                <style>%s</style></head>
                <body>%s</body></html>
                """.formatted(CONTENT_SECURITY_POLICY, style(theme), body(markdown));
    }

    private static String style(Theme theme) {
        boolean dark = theme == Theme.DARK;
        String background = dark ? "#0a0a0a" : "#ffffff";
        String text = dark ? "#f2f2f2" : "#000000";
        String rule = dark ? "#3a3a3a" : "#d6d6d6";
        String muted = dark ? "#a3a3a3" : "#555555";
        String codeBackground = dark ? "#1a1a1a" : "#f2f2f2";
        return STYLE.formatted(
                dark ? "dark" : "light",
                background,
                text,
                background,
                text,
                rule,
                codeBackground,
                codeBackground,
                rule,
                text,
                muted,
                rule);
    }
}
