package io.github.lukmnh.toolbox.devutils.sql.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlCheatsheet;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlSnippet;
import java.util.List;
import java.util.Locale;

/** Kunci messages cheatsheet/konsep SQL dirangkai di satu tempat supaya tidak tersebar sebagai string. */
final class SqlTexts {

    static final String PREFIX = "devutils.sql.";

    private SqlTexts() {}

    static String groupKey(String group) {
        return PREFIX + "group." + group;
    }

    static String introKey(String group) {
        return groupKey(group) + ".intro";
    }

    static String snippetKey(SqlSnippet snippet) {
        return PREFIX + "sheet." + snippet.key();
    }

    static String whenKey(SqlSnippet snippet) {
        return snippetKey(snippet) + ".when";
    }

    static String group(String group) {
        return Messages.get(groupKey(group));
    }

    static String intro(String group) {
        return Messages.get(introKey(group));
    }

    static String snippet(SqlSnippet snippet) {
        return Messages.get(snippetKey(snippet));
    }

    static String when(SqlSnippet snippet) {
        return Messages.get(whenKey(snippet));
    }

    /**
     * Snippet di grup tersebut yang cocok dengan query di kode, penjelasan, atau kapan dipakai. Query yang cocok
     * dengan judul grup menampilkan seluruh grup (mis. "deadlock").
     */
    static List<SqlSnippet> matching(String group, String query) {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        boolean wholeGroup = needle.isEmpty() || contains(group(group), needle);
        return SqlCheatsheet.SNIPPETS.stream()
                .filter(snippet -> snippet.group().equals(group))
                .filter(snippet -> wholeGroup
                        || contains(snippet.code(), needle)
                        || contains(snippet(snippet), needle)
                        || contains(when(snippet), needle))
                .toList();
    }

    private static boolean contains(String text, String needle) {
        return text.toLowerCase(Locale.ROOT).contains(needle);
    }
}
