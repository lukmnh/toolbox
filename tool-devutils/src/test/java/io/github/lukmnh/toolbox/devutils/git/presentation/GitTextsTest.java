package io.github.lukmnh.toolbox.devutils.git.presentation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.devutils.git.domain.GitArgument;
import io.github.lukmnh.toolbox.devutils.git.domain.GitCatalog;
import io.github.lukmnh.toolbox.devutils.git.domain.GitCheatsheet;
import io.github.lukmnh.toolbox.devutils.git.domain.GitCommandSpec;
import io.github.lukmnh.toolbox.devutils.git.domain.GitOption;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Key yang lupa ditulis tampil sebagai "!key!" di UI; test ini menangkapnya di build. */
class GitTextsTest {

    @Test
    void everyCatalogAndCheatsheetKeyHasAMessage() {
        List<String> keys = new ArrayList<>();
        for (GitCommandSpec spec : GitCatalog.COMMANDS) {
            keys.add(GitTexts.commandKey(spec));
            spec.arguments().stream().map(GitArgument::key).forEach(key -> keys.add(GitTexts.argumentKey(spec, key)));
            spec.options().stream().map(GitOption::key).forEach(key -> keys.add(GitTexts.optionKey(spec, key)));
        }
        GitCheatsheet.GROUPS.forEach(group -> keys.add(GitTexts.groupKey(group)));
        GitCheatsheet.SNIPPETS.forEach(snippet -> keys.add(GitTexts.snippetKey(snippet)));

        List<String> missing =
                keys.stream().filter(key -> Messages.get(key).startsWith("!")).toList();

        assertTrue(missing.isEmpty(), "Key tanpa teks: " + missing);
    }
}
