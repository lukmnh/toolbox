package io.github.lukmnh.toolbox.devutils.git.domain;

import java.util.List;

/** Hasil builder: command siap copy, token-tokennya untuk penjelasan, dan masalah input (kosong = valid). */
public record GitBuild(String command, List<GitPart> parts, List<GitIssue> issues) {

    public GitBuild {
        parts = List.copyOf(parts);
        issues = List.copyOf(issues);
    }
}
