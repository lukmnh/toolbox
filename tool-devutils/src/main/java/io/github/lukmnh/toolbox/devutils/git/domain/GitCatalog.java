package io.github.lukmnh.toolbox.devutils.git.domain;

import static io.github.lukmnh.toolbox.devutils.git.domain.GitArgument.optional;
import static io.github.lukmnh.toolbox.devutils.git.domain.GitArgument.required;
import static io.github.lukmnh.toolbox.devutils.git.domain.GitOption.flag;
import static io.github.lukmnh.toolbox.devutils.git.domain.GitOption.value;

import java.util.List;

/** Command yang tersedia di Builder. Teks penjelasan ada di messages: {@code devutils.git.cmd|arg|opt.<id>...}. */
public final class GitCatalog {

    public static final List<GitCommandSpec> COMMANDS = List.of(
            command(
                    "clone",
                    "clone",
                    List.of(required("url"), optional("directory")),
                    value("depth", "--depth"),
                    value("branch", "--branch"),
                    flag("recurse", "--recurse-submodules")),
            command(
                    "add",
                    "add",
                    List.of(optional("pathspec")),
                    flag("all", "-A"),
                    flag("patch", "-p"),
                    flag("update", "-u")),
            command(
                    "commit",
                    "commit",
                    List.of(),
                    value("message", "-m"),
                    flag("all", "-a"),
                    flag("amend", "--amend").asDangerous(),
                    flag("noEdit", "--no-edit"),
                    flag("signoff", "-s")),
            command(
                    "branch",
                    "branch",
                    List.of(optional("name")),
                    flag("delete", "-d"),
                    flag("forceDelete", "-D").asDangerous(),
                    flag("move", "-m"),
                    flag("all", "-a"),
                    flag("verbose", "-vv")),
            command("switch", "switch", List.of(required("branch")), flag("create", "-c"), flag("detach", "--detach")),
            command(
                    "merge",
                    "merge",
                    List.of(required("branch")),
                    flag("noFf", "--no-ff").inGroup("mode"),
                    flag("ffOnly", "--ff-only").inGroup("mode"),
                    flag("squash", "--squash").inGroup("mode")),
            command(
                    "rebase",
                    "rebase",
                    List.of(required("upstream")),
                    flag("interactive", "-i"),
                    value("onto", "--onto"),
                    flag("autosquash", "--autosquash")),
            command(
                    "pull",
                    "pull",
                    List.of(optional("remote"), optional("branch")),
                    flag("rebase", "--rebase").inGroup("mode"),
                    flag("ffOnly", "--ff-only").inGroup("mode")),
            command(
                    "push",
                    "push",
                    List.of(optional("remote"), optional("branch")),
                    flag("setUpstream", "-u"),
                    flag("forceWithLease", "--force-with-lease").inGroup("force"),
                    flag("force", "--force").inGroup("force").asDangerous(),
                    flag("tags", "--tags")),
            command(
                    "fetch",
                    "fetch",
                    List.of(optional("remote")),
                    flag("all", "--all"),
                    flag("prune", "--prune"),
                    flag("tags", "--tags")),
            command(
                    "stash",
                    "stash push",
                    List.of(),
                    value("message", "-m"),
                    flag("untracked", "-u"),
                    flag("keepIndex", "--keep-index")),
            command(
                    "log",
                    "log",
                    List.of(),
                    flag("oneline", "--oneline"),
                    flag("graph", "--graph"),
                    flag("all", "--all"),
                    value("maxCount", "-n"),
                    value("author", "--author"),
                    value("since", "--since")),
            command(
                    "diff",
                    "diff",
                    List.of(optional("target")),
                    flag("staged", "--staged"),
                    flag("stat", "--stat"),
                    flag("nameOnly", "--name-only")),
            command(
                    "reset",
                    "reset",
                    List.of(optional("commit")),
                    flag("soft", "--soft").inGroup("mode"),
                    flag("mixed", "--mixed").inGroup("mode"),
                    flag("hard", "--hard").inGroup("mode").asDangerous()),
            command(
                    "restore",
                    "restore",
                    List.of(required("pathspec")),
                    flag("staged", "--staged"),
                    value("source", "--source")),
            command(
                    "tag",
                    "tag",
                    List.of(required("name"), optional("commit")),
                    flag("annotate", "-a"),
                    value("message", "-m"),
                    flag("delete", "-d")),
            command(
                    "revert",
                    "revert",
                    List.of(required("commit")),
                    flag("noEdit", "--no-edit"),
                    flag("noCommit", "-n")),
            command(
                    "cherryPick",
                    "cherry-pick",
                    List.of(required("commit")),
                    flag("noCommit", "-n"),
                    flag("recordOrigin", "-x")));

    private GitCatalog() {}

    private static GitCommandSpec command(String id, String verb, List<GitArgument> arguments, GitOption... options) {
        return new GitCommandSpec(id, verb, arguments, List.of(options));
    }
}
