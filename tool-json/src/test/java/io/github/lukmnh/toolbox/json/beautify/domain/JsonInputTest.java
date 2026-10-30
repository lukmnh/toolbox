package io.github.lukmnh.toolbox.json.beautify.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.json.shared.domain.NotJsonFileException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JsonInputTest {

    @ParameterizedTest
    @ValueSource(strings = {"data.json", "DATA.JSON", "laporan.final.Json"})
    void acceptsJsonExtensionCaseInsensitive(String name) {
        assertTrue(JsonInput.FileInput.isJsonFile(Path.of(name)));
        assertEquals(10, new JsonInput.FileInput(Path.of(name), 10).sizeBytes());
    }

    @ParameterizedTest
    @ValueSource(strings = {"log.jsonl", "dump.ndjson", "data.txt", "json", "data.json.bak"})
    void rejectsOtherExtensions(String name) {
        assertFalse(JsonInput.FileInput.isJsonFile(Path.of(name)));
        assertThrows(NotJsonFileException.class, () -> new JsonInput.FileInput(Path.of(name), 1));
    }

    @Test
    void baseNameDropsOnlyJsonExtension() {
        assertEquals("laporan.final", new JsonInput.FileInput(Path.of("laporan.final.JSON"), 1).baseName());
    }

    @Test
    void textSizeIsUtf8ByteLength() {
        String text = "{\"n\":\"é😀日\"}";

        assertEquals(text.getBytes(StandardCharsets.UTF_8).length, new JsonInput.Text(text).sizeBytes());
    }

    @Test
    void outputNamesFollowAction() {
        assertEquals("data.pretty.json", BeautifyAction.BEAUTIFY.outputName("data"));
        assertEquals("data.min.json", BeautifyAction.MINIFY.outputName("data"));
        assertThrows(IllegalStateException.class, () -> BeautifyAction.VALIDATE.outputName("data"));
    }

    @Test
    void limitsFollowBusinessRules() {
        long mb = 1024L * 1024L;
        assertTrue(BeautifyLimits.canSortKeys(50 * mb));
        assertFalse(BeautifyLimits.canSortKeys(50 * mb + 1));
        assertTrue(BeautifyLimits.canCopy(10 * mb));
        assertFalse(BeautifyLimits.canCopy(10 * mb + 1));
    }
}
