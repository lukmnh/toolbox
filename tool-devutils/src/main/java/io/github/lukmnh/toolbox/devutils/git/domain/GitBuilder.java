package io.github.lukmnh.toolbox.devutils.git.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Menyusun command git: {@code git <verb> <opsi...> <argumen...>}. Hanya string; tidak pernah menjalankan git. */
public final class GitBuilder {

    // Karakter yang aman tanpa quote di shell POSIX dan PowerShell
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._/@:=+,~^-]+");

    private GitBuilder() {}

    public static GitBuild build(GitRequest request) {
        GitCommandSpec spec = request.command();
        List<GitPart> parts = new ArrayList<>();
        parts.add(new GitPart("git " + spec.verb(), GitPart.Kind.COMMAND, spec.id(), false));
        Map<String, List<String>> groups = new LinkedHashMap<>();
        for (GitOption option : spec.options()) {
            String value = request.values().getOrDefault(option.key(), "").strip();
            boolean enabled =
                    option.takesValue() ? !value.isEmpty() : request.flags().contains(option.key());
            if (!enabled) {
                continue;
            }
            String text = option.takesValue() ? option.flag() + " " + quote(value) : option.flag();
            parts.add(new GitPart(text, GitPart.Kind.OPTION, option.key(), option.dangerous()));
            if (!option.group().isEmpty()) {
                groups.computeIfAbsent(option.group(), group -> new ArrayList<>())
                        .add(option.key());
            }
        }
        List<GitIssue> issues = new ArrayList<>();
        groups.values().stream()
                .filter(keys -> keys.size() > 1)
                .forEach(keys -> issues.add(new GitIssue(GitIssue.Kind.CONFLICT, keys)));
        for (GitArgument argument : spec.arguments()) {
            String value = request.arguments().getOrDefault(argument.key(), "").strip();
            if (!value.isEmpty()) {
                parts.add(new GitPart(quote(value), GitPart.Kind.ARGUMENT, argument.key(), false));
            } else if (argument.required()) {
                issues.add(new GitIssue(GitIssue.Kind.MISSING_ARGUMENT, List.of(argument.key())));
            }
        }
        String command = parts.stream().map(GitPart::text).collect(Collectors.joining(" "));
        return new GitBuild(command, parts, issues);
    }

    /** Nilai aman dibiarkan; sisanya dibungkus {@code '...'} dengan {@code '} di-escape gaya POSIX. */
    static String quote(String value) {
        return SAFE.matcher(value).matches() ? value : "'" + value.replace("'", "'\\''") + "'";
    }
}
