package io.github.lukmnh.toolbox.devutils.mock.presentation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.devutils.mock.domain.InvalidMockSchemaException;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockLocale;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Key yang lupa ditulis tampil sebagai "!key!" di UI; test ini menangkapnya di build. */
class MockTextsTest {

    @Test
    void everyTypeLocaleAndErrorHasAMessage() {
        List<String> keys = new ArrayList<>();
        for (MockType type : MockType.values()) {
            keys.add(MockTexts.typeKey(type));
            if (type.hasOptions()) {
                keys.add(MockTexts.optionsKey(type));
            }
        }
        for (MockLocale locale : MockLocale.values()) {
            keys.add(MockTexts.localeKey(locale));
        }
        for (InvalidMockSchemaException.Reason reason : InvalidMockSchemaException.Reason.values()) {
            keys.add(MockTexts.errorKey(reason));
        }
        keys.add(MockTexts.PREFIX + "error.io");
        keys.add(MockTexts.PREFIX + "error.header");

        List<String> missing =
                keys.stream().filter(key -> Messages.get(key).startsWith("!")).toList();

        assertTrue(missing.isEmpty(), "Key tanpa teks: " + missing);
    }
}
