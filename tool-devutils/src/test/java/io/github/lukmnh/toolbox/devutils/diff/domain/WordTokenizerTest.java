package io.github.lukmnh.toolbox.devutils.diff.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class WordTokenizerTest {

    @Test
    void splitsWordsWhitespaceAndPunctuation() {
        assertEquals(
                List.of("int", " ", "total_2", "  ", "=", " ", "a", "+", "b", ";"),
                WordTokenizer.tokenize("int total_2  = a+b;"));
    }

    @Test
    void handlesUnicodeAndEmpty() {
        assertEquals(List.of("café", " ", "😀", "!"), WordTokenizer.tokenize("café 😀!"));
        assertEquals(List.of(), WordTokenizer.tokenize(""));
    }

    @Test
    void tokensRebuildTheOriginalText() {
        String text = "  x := f(y, 'z')\t// komentar ";
        assertEquals(text, String.join("", WordTokenizer.tokenize(text)));
    }
}
