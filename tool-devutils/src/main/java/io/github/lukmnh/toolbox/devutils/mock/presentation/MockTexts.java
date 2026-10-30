package io.github.lukmnh.toolbox.devutils.mock.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.devutils.mock.domain.InvalidMockSchemaException;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockIoException;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockLocale;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockSchemaValidator;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockToolException;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockType;
import javafx.stage.Window;

/** Kunci messages {@code devutils.mock.*} dan pemetaan error domain ke pesan, di satu tempat. */
final class MockTexts {

    static final String PREFIX = "devutils.mock.";

    private MockTexts() {}

    static String typeKey(MockType type) {
        return PREFIX + "type." + type.name();
    }

    static String optionsKey(MockType type) {
        return PREFIX + "options." + type.name();
    }

    static String localeKey(MockLocale locale) {
        return PREFIX + "locale." + locale.name();
    }

    static String errorKey(InvalidMockSchemaException.Reason reason) {
        return PREFIX + "error." + reason.name();
    }

    static String type(MockType type) {
        return Messages.get(typeKey(type));
    }

    /** Petunjuk format opsi; kosong untuk tipe tanpa opsi. */
    static String options(MockType type) {
        return type.hasOptions() ? Messages.get(optionsKey(type)) : "";
    }

    static String locale(MockLocale locale) {
        return Messages.get(localeKey(locale));
    }

    static String describe(MockToolException error) {
        return switch (error) {
            case InvalidMockSchemaException e -> Messages.format(errorKey(e.reason()), e.subject(), limit(e.reason()));
            case MockIoException e -> Messages.format(PREFIX + "error.io", e.fileName());
        };
    }

    static void show(Window owner, Throwable error) {
        if (error instanceof MockToolException mockError) {
            ErrorDialogs.show(owner, Messages.get(PREFIX + "error.header"), describe(mockError));
            return;
        }
        ErrorDialogs.showUnexpected(owner, error);
    }

    /** Batas yang disebut di pesan error ({1}); 0 bila pesan tidak memakainya. */
    private static int limit(InvalidMockSchemaException.Reason reason) {
        return switch (reason) {
            case ROWS_OUT_OF_RANGE -> MockSchemaValidator.MAX_ROWS;
            case TOO_MANY_COLUMNS -> MockSchemaValidator.MAX_COLUMNS;
            case INVALID_PATTERN -> MockSchemaValidator.MAX_PATTERN_LENGTH;
            default -> 0;
        };
    }
}
