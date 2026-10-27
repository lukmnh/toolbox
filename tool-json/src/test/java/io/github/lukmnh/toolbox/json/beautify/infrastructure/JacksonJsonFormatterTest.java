package io.github.lukmnh.toolbox.json.beautify.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.json.beautify.domain.BeautifyAction;
import io.github.lukmnh.toolbox.json.beautify.domain.FormatOptions;
import io.github.lukmnh.toolbox.json.beautify.domain.FormatStats;
import io.github.lukmnh.toolbox.json.beautify.domain.Indent;
import io.github.lukmnh.toolbox.json.beautify.domain.JsonInput;
import io.github.lukmnh.toolbox.json.beautify.domain.TextPreview;
import io.github.lukmnh.toolbox.json.shared.domain.MalformedJsonException;
import io.github.lukmnh.toolbox.json.shared.domain.ScanProgress;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JacksonJsonFormatterTest {

    private static final String SAMPLE = "{\"b\":[1,2.50,{}],\"a\":{\"z\":null,\"y\":\"é😀\"},\"c\":[]}";

    private final JacksonJsonFormatter formatter = new JacksonJsonFormatter();

    @TempDir
    Path temp;

    @Test
    void beautifyWithTwoSpacesKeepsOrderAndNumberText() throws IOException {
        String pretty = run(SAMPLE, BeautifyAction.BEAUTIFY, Indent.TWO_SPACES, false);

        assertEquals("""
                {
                  "b": [
                    1,
                    2.50,
                    {}
                  ],
                  "a": {
                    "z": null,
                    "y": "é😀"
                  },
                  "c": []
                }
                """, pretty);
    }

    @Test
    void indentOptionsUseFourSpacesOrTab() throws IOException {
        assertTrue(run("{\"a\":1}", BeautifyAction.BEAUTIFY, Indent.FOUR_SPACES, false)
                .contains("\n    \"a\": 1"));
        assertTrue(run("{\"a\":1}", BeautifyAction.BEAUTIFY, Indent.TAB, false).contains("\n\t\"a\": 1"));
    }

    @Test
    void minifyRemovesWhitespaceButKeepsStringContent() throws IOException {
        String minified =
                run("{ \"a\" : [ 1 , 2 ] ,\n \"s\": \"x y\" }", BeautifyAction.MINIFY, Indent.TWO_SPACES, false);

        assertEquals("{\"a\":[1,2],\"s\":\"x y\"}", minified);
    }

    @Test
    void beautifyThenMinifyRoundTripsToOriginalMinified() throws IOException {
        String pretty = run(SAMPLE, BeautifyAction.BEAUTIFY, Indent.TAB, false);

        assertEquals(SAMPLE, run(pretty, BeautifyAction.MINIFY, Indent.TWO_SPACES, false));
    }

    @Test
    void sortKeysOrdersEveryObjectLevel() throws IOException {
        String sorted = run(SAMPLE, BeautifyAction.MINIFY, Indent.TWO_SPACES, true);

        assertEquals("{\"a\":{\"y\":\"é😀\",\"z\":null},\"b\":[1,2.50,{}],\"c\":[]}", sorted);
    }

    @Test
    void bigNumbersAreCopiedExactly() throws IOException {
        String json = "[12345678901234567890123,0.1000000000000000055511151231257827,1e400]";

        assertEquals(json, run(json, BeautifyAction.MINIFY, Indent.TWO_SPACES, false));
        assertEquals(json, run(json, BeautifyAction.MINIFY, Indent.TWO_SPACES, true));
    }

    @Test
    void countsKeysAndArrayElements() {
        FormatStats stats = formatter.format(
                new JsonInput.Text(SAMPLE),
                new FormatOptions(BeautifyAction.VALIDATE, Indent.TWO_SPACES, false),
                null,
                ScanProgress.NONE);

        assertEquals(5, stats.keyCount());
        assertEquals(3, stats.elementCount());
    }

    @Test
    void validateReportsLineColumnAndContextForText() {
        MalformedJsonException error = assertThrows(
                MalformedJsonException.class, () -> validate(new JsonInput.Text("{\n  \"a\": 1,\n  \"b\": tru\n}")));

        assertEquals(MalformedJsonException.Kind.SYNTAX, error.kind());
        assertEquals(3, error.line());
        assertTrue(error.sourceName().isEmpty());
        assertTrue(
                error.snippet().text().contains("\"b\": tru"), error.snippet().text());
    }

    @Test
    void validateReportsFileNameForFileInput() throws IOException {
        Path file = Files.writeString(temp.resolve("rusak.json"), "[1, 2,, 3]");

        MalformedJsonException error = assertThrows(
                MalformedJsonException.class, () -> validate(new JsonInput.FileInput(file, Files.size(file))));

        assertEquals("rusak.json", error.sourceName().orElseThrow());
        assertEquals(1, error.line());
        assertEquals(',', error.snippet().text().charAt(error.snippet().marker()));
    }

    @Test
    void emptyAndTrailingContentAreInvalid() {
        assertEquals(
                MalformedJsonException.Kind.EMPTY,
                assertThrows(MalformedJsonException.class, () -> validate(new JsonInput.Text("  \n ")))
                        .kind());
        MalformedJsonException trailing =
                assertThrows(MalformedJsonException.class, () -> validate(new JsonInput.Text("{} {}")));
        assertEquals(MalformedJsonException.Kind.TRAILING_CONTENT, trailing.kind());
        assertEquals('{', trailing.snippet().text().charAt(trailing.snippet().marker()));
    }

    @Test
    void scalarRootIsValidJson() {
        validate(new JsonInput.Text("\"halo\""));
        validate(new JsonInput.Text("42"));
    }

    @Test
    void readTextTruncatesOnCharacterBoundary() throws IOException {
        Path file = Files.writeString(temp.resolve("out.json"), "ab😀cd");

        TextPreview cut = formatter.readText(file, 4);
        TextPreview whole = formatter.readText(file, 100);

        assertEquals("ab", cut.text());
        assertTrue(cut.truncated());
        assertEquals("ab😀cd", whole.text());
        assertFalse(whole.truncated());
    }

    @Test
    void fileInputIsStreamedToOutput() throws IOException {
        Path source = Files.writeString(temp.resolve("data.json"), SAMPLE);
        Path output = temp.resolve("data.min.json");

        formatter.format(
                new JsonInput.FileInput(source, Files.size(source)),
                new FormatOptions(BeautifyAction.MINIFY, Indent.TWO_SPACES, false),
                output,
                ScanProgress.NONE);

        assertEquals(SAMPLE, Files.readString(output));
        assertEquals(SAMPLE, Files.readString(source));
    }

    private String run(String json, BeautifyAction action, Indent indent, boolean sortKeys) throws IOException {
        Path output = Files.createTempFile(temp, "out", ".json");
        formatter.format(
                new JsonInput.Text(json), new FormatOptions(action, indent, sortKeys), output, ScanProgress.NONE);
        return Files.readString(output);
    }

    private void validate(JsonInput input) {
        formatter.format(
                input, new FormatOptions(BeautifyAction.VALIDATE, Indent.TWO_SPACES, false), null, ScanProgress.NONE);
    }
}
