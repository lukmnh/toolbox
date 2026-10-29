package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.ArrayList;
import java.util.List;

/** Aturan {@code regexp.Regexp.Expand} Go, termasuk {@code $1x} yang dibaca sebagai nama "1x". */
final class GoReplacementParser {

    /** Go menolak nomor grup ≥ 10^8 dan memperlakukannya sebagai nama. */
    private static final long MAX_GROUP_NUMBER = 100_000_000L;

    private final String template;
    private final List<String> groupNames;
    private final List<ReplacementTemplate.Part> parts = new ArrayList<>();
    private int cursor;

    GoReplacementParser(String template, List<String> groupNames) {
        this.template = template;
        this.groupNames = groupNames;
    }

    List<ReplacementTemplate.Part> parse() {
        while (cursor < template.length()) {
            int dollar = template.indexOf('$', cursor);
            if (dollar < 0) {
                text(template.substring(cursor));
                cursor = template.length();
                return parts;
            }
            text(template.substring(cursor, dollar));
            cursor = dollar + 1;
            if (cursor < template.length() && template.charAt(cursor) == '$') {
                text("$");
                cursor++;
            } else if (!extractReference()) {
                // Referensi rusak: Go menulis "$" apa adanya lalu melanjutkan setelahnya
                text("$");
            }
        }
        return parts;
    }

    /** Salinan fungsi {@code extract} di paket regexp Go. */
    private boolean extractReference() {
        int index = cursor;
        boolean brace = index < template.length() && template.charAt(index) == '{';
        if (brace) {
            index++;
        }
        int nameStart = index;
        while (index < template.length()) {
            int codePoint = template.codePointAt(index);
            if (!Character.isLetter(codePoint) && !Character.isDigit(codePoint) && codePoint != '_') {
                break;
            }
            index += Character.charCount(codePoint);
        }
        if (index == nameStart) {
            return false;
        }
        String name = template.substring(nameStart, index);
        if (brace) {
            if (index >= template.length() || template.charAt(index) != '}') {
                return false;
            }
            index++;
        }
        cursor = index;
        parts.add(resolve(name));
        return true;
    }

    private ReplacementTemplate.Part resolve(String name) {
        long number = groupNumber(name);
        if (number >= 0) {
            return number < groupNames.size()
                    ? new ReplacementTemplate.Part.Group((int) number)
                    : new ReplacementTemplate.Part.Text("");
        }
        // Grup 0 bernama kosong, jadi nama non-kosong tidak pernah cocok dengan seluruh match
        int found = groupNames.indexOf(name);
        return found > 0 ? new ReplacementTemplate.Part.Group(found) : new ReplacementTemplate.Part.Text("");
    }

    /** Nomor bila nama hanya digit ASCII tanpa nol di depan; selain itu -1 (diperlakukan sebagai nama). */
    private static long groupNumber(String name) {
        long number = 0;
        for (int index = 0; index < name.length(); index++) {
            char ch = name.charAt(index);
            if (ch < '0' || ch > '9' || number >= MAX_GROUP_NUMBER) {
                return -1;
            }
            number = number * 10 + (ch - '0');
        }
        if (name.charAt(0) == '0' && name.length() > 1) {
            return -1;
        }
        return number;
    }

    private void text(String value) {
        if (!value.isEmpty()) {
            parts.add(new ReplacementTemplate.Part.Text(value));
        }
    }
}
