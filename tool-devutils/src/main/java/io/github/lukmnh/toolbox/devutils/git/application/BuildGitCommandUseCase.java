package io.github.lukmnh.toolbox.devutils.git.application;

import io.github.lukmnh.toolbox.devutils.git.domain.GitBuild;
import io.github.lukmnh.toolbox.devutils.git.domain.GitBuilder;
import io.github.lukmnh.toolbox.devutils.git.domain.GitRequest;

/** Builder: pilihan user → command git + token untuk penjelasan + masalah input. */
public final class BuildGitCommandUseCase {

    public GitBuild execute(GitRequest request) {
        return GitBuilder.build(request);
    }
}
