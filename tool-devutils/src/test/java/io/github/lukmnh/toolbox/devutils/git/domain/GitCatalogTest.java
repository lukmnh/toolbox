package io.github.lukmnh.toolbox.devutils.git.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class GitCatalogTest {

    @Test
    void commandIdsAreUnique() {
        List<String> ids = GitCatalog.COMMANDS.stream().map(GitCommandSpec::id).toList();

        assertEquals(ids.size(), new HashSet<>(ids).size());
    }

    /** Argumen dan opsi berbagi namespace key di GitRequest/messages, jadi harus unik per command. */
    @Test
    void keysAreUniqueWithinEachCommand() {
        for (GitCommandSpec spec : GitCatalog.COMMANDS) {
            List<String> keys = Stream.concat(
                            spec.arguments().stream().map(GitArgument::key),
                            spec.options().stream().map(GitOption::key))
                    .toList();
            Set<String> unique = new HashSet<>(keys);

            assertEquals(keys.size(), unique.size(), spec.id());
        }
    }

    @Test
    void cheatsheetGroupsAreAllListed() {
        assertTrue(GitCheatsheet.SNIPPETS.stream().map(GitSnippet::group).allMatch(GitCheatsheet.GROUPS::contains));
    }
}
