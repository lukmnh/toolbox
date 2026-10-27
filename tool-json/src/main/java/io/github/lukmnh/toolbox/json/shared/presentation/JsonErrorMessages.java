package io.github.lukmnh.toolbox.json.shared.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.json.shared.domain.ErrorSnippet;
import io.github.lukmnh.toolbox.json.shared.domain.InvalidQueryException;
import io.github.lukmnh.toolbox.json.shared.domain.JsonIoException;
import io.github.lukmnh.toolbox.json.shared.domain.JsonToolException;
import io.github.lukmnh.toolbox.json.shared.domain.MalformedJsonException;
import io.github.lukmnh.toolbox.json.shared.domain.NotJsonFileException;
import java.nio.file.Path;
import javafx.stage.Window;

/** Memetakan error domain JSON ke pesan berbahasa Indonesia yang menyebut nama file dan posisi. */
public final class JsonErrorMessages {

    private static final String MARKER = "^";

    private JsonErrorMessages() {}

    public static void show(Window owner, Throwable error) {
        if (error instanceof JsonToolException jsonError) {
            ErrorDialogs.show(owner, header(jsonError), detail(jsonError));
            return;
        }
        ErrorDialogs.showUnexpected(owner, error);
    }

    public static String header(JsonToolException error) {
        return switch (error) {
            case MalformedJsonException e ->
                e.sourceName()
                        .map(name -> Messages.format("json.error.malformed.file", name, e.line(), e.column()))
                        .orElseGet(() -> Messages.format("json.error.malformed.text", e.line(), e.column()));
            case JsonIoException e -> Messages.format("json.error.io", name(e.file()));
            case NotJsonFileException e -> Messages.format("json.error.notJson", name(e.file()));
            case InvalidQueryException e -> Messages.format("json.error.query.header", e.position() + 1);
        };
    }

    public static String detail(JsonToolException error) {
        return switch (error) {
            case MalformedJsonException e -> withSnippet(reason(e), e.snippet());
            case InvalidQueryException e -> queryReason(e);
            case NotJsonFileException e -> Messages.get("json.error.notJson.detail");
            case JsonIoException e -> ErrorDialogs.describe(e);
        };
    }

    /** Dua baris: potongan teks dan penanda {@code ^} di posisi error (untuk font monospace). */
    public static String snippet(ErrorSnippet snippet) {
        return snippet.text() + "\n" + " ".repeat(snippet.marker()) + MARKER;
    }

    private static String reason(MalformedJsonException error) {
        return switch (error.kind()) {
            case EMPTY -> Messages.get("json.error.malformed.empty");
            case TRAILING_CONTENT -> Messages.get("json.error.malformed.trailing");
            case SYNTAX -> Messages.format("json.error.malformed.syntax", error.detail());
        };
    }

    private static String queryReason(InvalidQueryException error) {
        String key = switch (error.reason()) {
            case EMPTY -> "json.error.query.empty";
            case UNEXPECTED_TOKEN -> "json.error.query.unexpected";
            case EXPECTED_FIELD -> "json.error.query.expectedField";
            case EXPECTED_OPERATOR -> "json.error.query.expectedOperator";
            case EXPECTED_VALUE -> "json.error.query.expectedValue";
            case UNCLOSED_STRING -> "json.error.query.unclosedString";
            case UNCLOSED_PARENTHESIS -> "json.error.query.unclosedParenthesis";
            case INVALID_PATH -> "json.error.query.invalidPath";
        };
        return Messages.format(key, error.token());
    }

    private static String withSnippet(String text, ErrorSnippet snippet) {
        return snippet.isEmpty() ? text : text + "\n\n" + snippet(snippet);
    }

    private static String name(Path file) {
        return file.getFileName().toString();
    }
}
