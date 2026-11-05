package io.github.lukmnh.toolbox.devutils.sql.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlCheatsheet;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlSnippet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Key yang lupa ditulis tampil sebagai "!key!" di UI; test ini menangkapnya di build. */
class SqlTextsTest {

    @Test
    void everyGroupIntroAndSnippetKeyHasAMessage() {
        List<String> keys = new ArrayList<>();
        Stream.concat(SqlCheatsheet.SYNTAX_GROUPS.stream(), SqlCheatsheet.CONCEPT_GROUPS.stream())
                .forEach(group -> keys.add(SqlTexts.groupKey(group)));
        SqlCheatsheet.CONCEPT_GROUPS.forEach(group -> keys.add(SqlTexts.introKey(group)));
        for (SqlSnippet snippet : SqlCheatsheet.SNIPPETS) {
            keys.add(SqlTexts.titleKey(snippet));
            keys.add(SqlTexts.snippetKey(snippet));
            keys.add(SqlTexts.whenKey(snippet));
        }

        List<String> missing =
                keys.stream().filter(key -> Messages.get(key).startsWith("!")).toList();

        assertTrue(missing.isEmpty(), "Key tanpa teks: " + missing);
    }

    @Test
    void everySnippetBelongsToAKnownGroupAndHasAUniqueKey() {
        Set<String> groups = new HashSet<>(SqlCheatsheet.SYNTAX_GROUPS);
        groups.addAll(SqlCheatsheet.CONCEPT_GROUPS);
        Set<String> keys = new HashSet<>();

        for (SqlSnippet snippet : SqlCheatsheet.SNIPPETS) {
            assertTrue(groups.contains(snippet.group()), "Grup tidak dikenal: " + snippet.group());
            assertTrue(keys.add(snippet.key()), "Key duplikat: " + snippet.key());
        }
    }

    @Test
    void emptyQueryReturnsTheWholeGroup() {
        long expected = SqlCheatsheet.SNIPPETS.stream()
                .filter(snippet -> snippet.group().equals("locking"))
                .count();

        assertEquals(expected, SqlTexts.matching("locking", "  ").size());
    }

    @Test
    void queryMatchesCodeCaseInsensitively() {
        List<SqlSnippet> matches = SqlTexts.matching("locking", "skip locked");

        assertEquals(
                List.of("skipLocked"), matches.stream().map(SqlSnippet::key).toList());
    }

    @Test
    void queryMatchingTheGroupTitleReturnsTheWholeGroup() {
        assertEquals(
                SqlTexts.matching("deadlock", "").size(),
                SqlTexts.matching("deadlock", "DEADLOCKS").size());
    }

    @Test
    void unrelatedQueryReturnsNothing() {
        assertFalse(SqlTexts.matching("join", "zzz-no-such-text").iterator().hasNext());
    }
}
