package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.ArrayList;
import java.util.List;

/** Aturan {@code GetSubstitution} ECMAScript: referensi tidak valid menjadi teks literal, bukan error. */
final class JavaScriptReplacementParser {

    private static final int MAX_TWO_DIGIT = 99;

    private final String template;
    private final List<String> groupNames;
    private final int groupCount;
    private final boolean hasNamedGroups;
    private final List<ReplacementTemplate.Part> parts = new ArrayList<>();
    private int cursor;

    JavaScriptReplacementParser(String template, List<String> groupNames) {
        this.template = template;
        this.groupNames = groupNames;
        this.groupCount = groupNames.size() - 1;
        this.hasNamedGroups = groupNames.stream().skip(1).anyMatch(name -> !name.isEmpty());
    }

    List<ReplacementTemplate.Part> parse() {
        while (cursor < template.length()) {
            char current = template.charAt(cursor);
            if (current == '$' && cursor + 1 < template.length()) {
                dollar(template.charAt(cursor + 1));
            } else {
                literal(1);
            }
        }
        return parts;
    }

    private void dollar(char next) {
        switch (next) {
            case '$' -> {
                parts.add(new ReplacementTemplate.Part.Text("$"));
                cursor += 2;
            }
            case '&' -> special(new ReplacementTemplate.Part.WholeMatch());
            case '`' -> special(new ReplacementTemplate.Part.Before());
            case '\'' -> special(new ReplacementTemplate.Part.After());
            case '<' -> named();
            default -> {
                if (isDigit(next)) {
                    numbered();
                } else {
                    literal(1);
                }
            }
        }
    }

    private void special(ReplacementTemplate.Part part) {
        parts.add(part);
        cursor += 2;
    }

    /** Dua digit bila nomornya ada, kalau tidak satu digit, kalau tidak teks literal. */
    private void numbered() {
        boolean twoDigits = cursor + 2 < template.length() && isDigit(template.charAt(cursor + 2));
        int digitCount = twoDigits ? 2 : 1;
        int index = Integer.parseInt(template.substring(cursor + 1, cursor + 1 + digitCount));
        if (index > groupCount && digitCount == 2) {
            digitCount = 1;
            index = template.charAt(cursor + 1) - '0';
        }
        if (index >= 1 && index <= groupCount && index <= MAX_TWO_DIGIT) {
            parts.add(new ReplacementTemplate.Part.Group(index));
            cursor += 1 + digitCount;
            return;
        }
        literal(1 + digitCount);
    }

    /** {@code $<nama>} hanya bermakna bila pola punya named group; nama tidak dikenal → string kosong. */
    private void named() {
        int close = template.indexOf('>', cursor + 2);
        if (!hasNamedGroups || close < 0) {
            literal(2);
            return;
        }
        String name = template.substring(cursor + 2, close);
        int number = name.isEmpty() ? -1 : groupNames.indexOf(name);
        parts.add(number > 0 ? new ReplacementTemplate.Part.Group(number) : new ReplacementTemplate.Part.Text(""));
        cursor = close + 1;
    }

    private void literal(int length) {
        parts.add(new ReplacementTemplate.Part.Text(template.substring(cursor, cursor + length)));
        cursor += length;
    }

    private static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }
}
