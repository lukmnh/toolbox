package io.github.lukmnh.toolbox.devutils.git.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GitBuilderTest {

    @Test
    void optionsComeBeforeArgumentsAndValuesAreQuoted() {
        GitBuild build = GitBuilder.build(
                new GitRequest(spec("commit"), Map.of(), Set.of("all"), Map.of("message", "fix login bug")));

        assertEquals("git commit -m 'fix login bug' -a", build.command());
        assertTrue(build.issues().isEmpty());
    }

    @Test
    void argumentsFollowOptions() {
        GitBuild build = GitBuilder.build(new GitRequest(
                spec("push"), Map.of("remote", "origin", "branch", "feature/login"), Set.of("setUpstream"), Map.of()));

        assertEquals("git push -u origin feature/login", build.command());
    }

    @Test
    void blankValueOptionIsSkipped() {
        GitBuild build =
                GitBuilder.build(new GitRequest(spec("log"), Map.of(), Set.of("oneline"), Map.of("author", "  ")));

        assertEquals("git log --oneline", build.command());
    }

    @Test
    void missingRequiredArgumentIsReported() {
        GitBuild build = GitBuilder.build(new GitRequest(spec("switch"), Map.of(), Set.of(), Map.of()));

        assertEquals(List.of(new GitIssue(GitIssue.Kind.MISSING_ARGUMENT, List.of("branch"))), build.issues());
    }

    @Test
    void exclusiveOptionsAreReportedAsConflict() {
        GitBuild build = GitBuilder.build(new GitRequest(spec("reset"), Map.of(), Set.of("soft", "hard"), Map.of()));

        assertEquals(List.of(new GitIssue(GitIssue.Kind.CONFLICT, List.of("soft", "hard"))), build.issues());
    }

    @Test
    void destructiveOptionIsMarked() {
        GitBuild build = GitBuilder.build(new GitRequest(spec("push"), Map.of(), Set.of("force"), Map.of()));

        assertTrue(build.parts().stream().anyMatch(part -> part.text().equals("--force") && part.dangerous()));
    }

    @Test
    void quoteLeavesSafeValuesAndEscapesSingleQuotes() {
        assertEquals("HEAD~1", GitBuilder.quote("HEAD~1"));
        assertEquals("'it'\\''s done'", GitBuilder.quote("it's done"));
        assertEquals("'a;rm -rf'", GitBuilder.quote("a;rm -rf"));
    }

    private static GitCommandSpec spec(String id) {
        return GitCatalog.COMMANDS.stream()
                .filter(command -> command.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
