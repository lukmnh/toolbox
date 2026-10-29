package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Aturan {@code preg_replace} PHP (fungsi {@code preg_get_backref} di php_pcre.c): {@code \n}, {@code $n},
 * {@code ${n}} maksimal dua digit; {@code \} atau {@code $} setelah backslash literal meng-escape backslash itu.
 */
final class PhpReplacementParser {

    private static final char NO_PREVIOUS = 0;

    private final String template;
    private final List<ReplacementTemplate.Part> parts = new ArrayList<>();
    private int cursor;
    /** Karakter literal terakhir yang disalin; PHP memakainya untuk mendeteksi escape. */
    private char previous = NO_PREVIOUS;

    PhpReplacementParser(String template) {
        this.template = template;
    }

    List<ReplacementTemplate.Part> parse() {
        while (cursor < template.length()) {
            char current = template.charAt(cursor);
            boolean special = current == '\\' || current == '$';
            if (special && previous == '\\') {
                replacePreviousBackslash(current);
            } else if (!special || !backReference()) {
                parts.add(new ReplacementTemplate.Part.Text(String.valueOf(current)));
                previous = current;
                cursor++;
            }
        }
        return parts;
    }

    /** {@code \\} → {@code \}, {@code \$} → {@code $}: backslash yang sudah disalin diganti karakter ini. */
    private void replacePreviousBackslash(char current) {
        parts.set(parts.size() - 1, new ReplacementTemplate.Part.Text(String.valueOf(current)));
        previous = NO_PREVIOUS;
        cursor++;
    }

    private boolean backReference() {
        if (cursor + 1 >= template.length()) {
            return false;
        }
        int index = cursor;
        boolean brace = template.charAt(index) == '$' && template.charAt(index + 1) == '{';
        if (brace) {
            index++;
        }
        index++;
        if (index >= template.length() || !isDigit(template.charAt(index))) {
            return false;
        }
        int number = template.charAt(index) - '0';
        index++;
        if (index < template.length() && isDigit(template.charAt(index))) {
            number = number * 10 + (template.charAt(index) - '0');
            index++;
        }
        if (brace) {
            if (index >= template.length() || template.charAt(index) != '}') {
                return false;
            }
            index++;
        }
        parts.add(new ReplacementTemplate.Part.Group(number));
        cursor = index;
        return true;
    }

    private static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }
}
