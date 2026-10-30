package io.github.lukmnh.toolbox.devutils.git.domain;

import java.util.List;

/** Referensi syntax git per grup; urutan list = urutan tampil. */
public final class GitCheatsheet {

    public static final List<GitSnippet> SNIPPETS = List.of(
            new GitSnippet("setup", "git config --global user.name 'Your Name'", "configName"),
            new GitSnippet("setup", "git config --global user.email you@example.com", "configEmail"),
            new GitSnippet("setup", "git init", "init"),
            new GitSnippet("setup", "git clone <url>", "clone"),
            new GitSnippet("inspect", "git status -sb", "status"),
            new GitSnippet("inspect", "git log --oneline --graph --all", "logGraph"),
            new GitSnippet("inspect", "git diff", "diff"),
            new GitSnippet("inspect", "git diff --staged", "diffStaged"),
            new GitSnippet("inspect", "git show <commit>", "show"),
            new GitSnippet("inspect", "git blame <file>", "blame"),
            new GitSnippet("branch", "git branch -a", "branchList"),
            new GitSnippet("branch", "git switch -c <branch>", "branchCreate"),
            new GitSnippet("branch", "git switch <branch>", "branchSwitch"),
            new GitSnippet("branch", "git branch -d <branch>", "branchDelete"),
            new GitSnippet("branch", "git merge --no-ff <branch>", "merge"),
            new GitSnippet("branch", "git rebase main", "rebase"),
            new GitSnippet("commit", "git add -A", "addAll"),
            new GitSnippet("commit", "git add -p", "addPatch"),
            new GitSnippet("commit", "git commit -m 'message'", "commit"),
            new GitSnippet("commit", "git commit --amend --no-edit", "amend"),
            new GitSnippet("remote", "git remote -v", "remoteList"),
            new GitSnippet("remote", "git fetch --prune", "fetch"),
            new GitSnippet("remote", "git pull --rebase", "pull"),
            new GitSnippet("remote", "git push -u origin <branch>", "pushUpstream"),
            new GitSnippet("remote", "git push --force-with-lease", "pushLease"),
            new GitSnippet("undo", "git restore <file>", "restore"),
            new GitSnippet("undo", "git restore --staged <file>", "unstage"),
            new GitSnippet("undo", "git reset --soft HEAD~1", "resetSoft"),
            new GitSnippet("undo", "git reset --hard HEAD", "resetHard"),
            new GitSnippet("undo", "git revert <commit>", "revert"),
            new GitSnippet("undo", "git reflog", "reflog"),
            new GitSnippet("stash", "git stash push -u -m 'wip'", "stash"),
            new GitSnippet("stash", "git stash list", "stashList"),
            new GitSnippet("stash", "git stash pop", "stashPop"),
            new GitSnippet("stash", "git stash apply stash@{0}", "stashApply"),
            new GitSnippet("stash", "git stash drop stash@{0}", "stashDrop"),
            new GitSnippet("stash", "git stash branch <branch>", "stashBranch"));

    /** Urutan grup di tampilan. */
    public static final List<String> GROUPS =
            List.of("setup", "inspect", "branch", "commit", "remote", "undo", "stash");

    private GitCheatsheet() {}
}
