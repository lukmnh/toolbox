package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.ArrayList;
import java.util.List;

/** Aturan {@code java.util.regex.Matcher.appendReplacement}: referensi tidak valid adalah error. */
final class JavaReplacementParser {

    private final String template;
    private final List<String> groupNames;
    private final int groupCount;
    private final List<ReplacementTemplate.Part> parts = new ArrayList<>();
    private int cursor;

    JavaReplacementParser(String template, List<String> groupNames) {
        this.template = template;
        this.groupNames = groupNames;
        this.groupCount = groupNames.size() - 1;
    }

    List<ReplacementTemplate.Part> parse() {
        while (cursor < template.length()) {
            char current = template.charAt(cursor);
            if (current == '\\') {
                escape();
            } else if (current == '$') {
                reference();
            } else {
                parts.add(new ReplacementTemplate.Part.Text(String.valueOf(current)));
                cursor++;
            }
        }
        return parts;
    }

    private void escape() {
        int start = cursor;
        cursor++;
        if (cursor >= template.length()) {
            throw new InvalidReplacementException(start, "\\");
        }
        parts.add(new ReplacementTemplate.Part.Text(String.valueOf(template.charAt(cursor))));
        cursor++;
    }

    private void reference() {
        int start = cursor;
        cursor++;
        if (cursor >= template.length()) {
            throw new InvalidReplacementException(start, "$");
        }
        char next = template.charAt(cursor);
        if (next == '{') {
            namedReference(start);
            return;
        }
        if (!isAsciiDigit(next)) {
            throw new InvalidReplacementException(start, template.substring(start, cursor + 1));
        }
        numberedReference(start);
    }

    /** Digit pertama selalu nomor grup; digit berikutnya diambil selama nomornya masih ≤ jumlah grup. */
    private void numberedReference(int start) {
        int number = template.charAt(cursor) - '0';
        cursor++;
        while (cursor < template.length() && isAsciiDigit(template.charAt(cursor))) {
            int longer = number * 10 + (template.charAt(cursor) - '0');
            if (longer > groupCount) {
                break;
            }
            number = longer;
            cursor++;
        }
        if (number > groupCount) {
            throw new InvalidReplacementException(start, template.substring(start, cursor));
        }
        parts.add(new ReplacementTemplate.Part.Group(number));
    }

    private void namedReference(int start) {
        int close = template.indexOf('}', cursor);
        if (close < 0) {
            throw new InvalidReplacementException(start, template.substring(start));
        }
        String name = template.substring(cursor + 1, close);
        cursor = close + 1;
        int number = groupNames.indexOf(name);
        if (name.isEmpty() || !isValidName(name) || number <= 0) {
            throw new InvalidReplacementException(start, template.substring(start, cursor));
        }
        parts.add(new ReplacementTemplate.Part.Group(number));
    }

    /** Nama grup Java: huruf ASCII diikuti huruf/angka ASCII. */
    private static boolean isValidName(String name) {
        if (!isAsciiLetter(name.charAt(0))) {
            return false;
        }
        return name.chars().allMatch(ch -> isAsciiLetter((char) ch) || isAsciiDigit((char) ch));
    }

    private static boolean isAsciiLetter(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
    }

    private static boolean isAsciiDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }
}
