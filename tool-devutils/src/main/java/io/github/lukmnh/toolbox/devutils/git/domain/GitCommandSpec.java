package io.github.lukmnh.toolbox.devutils.git.domain;

import java.util.List;

/**
 * @param id key stabil untuk teks penjelasan
 * @param verb subcommand yang ditulis setelah {@code git}, misalnya {@code stash push}
 */
public record GitCommandSpec(String id, String verb, List<GitArgument> arguments, List<GitOption> options) {

    public GitCommandSpec {
        arguments = List.copyOf(arguments);
        options = List.copyOf(options);
    }
}
