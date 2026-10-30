package io.github.lukmnh.toolbox.devutils.git.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.devutils.git.domain.GitCommandSpec;
import io.github.lukmnh.toolbox.devutils.git.domain.GitPart;
import io.github.lukmnh.toolbox.devutils.git.domain.GitSnippet;

/** Kunci messages {@code devutils.git.*} dirangkai di satu tempat supaya tidak tersebar sebagai string. */
final class GitTexts {

    static final String PREFIX = "devutils.git.";

    private GitTexts() {}

    static String commandKey(GitCommandSpec spec) {
        return PREFIX + "cmd." + spec.id();
    }

    static String argumentKey(GitCommandSpec spec, String key) {
        return PREFIX + "arg." + spec.id() + "." + key;
    }

    static String optionKey(GitCommandSpec spec, String key) {
        return PREFIX + "opt." + spec.id() + "." + key;
    }

    static String groupKey(String group) {
        return PREFIX + "group." + group;
    }

    static String snippetKey(GitSnippet snippet) {
        return PREFIX + "sheet." + snippet.key();
    }

    static String command(GitCommandSpec spec) {
        return Messages.get(commandKey(spec));
    }

    static String argument(GitCommandSpec spec, String key) {
        return Messages.get(argumentKey(spec, key));
    }

    static String option(GitCommandSpec spec, String key) {
        return Messages.get(optionKey(spec, key));
    }

    static String group(String group) {
        return Messages.get(groupKey(group));
    }

    static String snippet(GitSnippet snippet) {
        return Messages.get(snippetKey(snippet));
    }

    /** Penjelasan token sesuai asalnya (command, opsi, argumen). */
    static String part(GitCommandSpec spec, GitPart part) {
        return switch (part.kind()) {
            case COMMAND -> command(spec);
            case OPTION -> option(spec, part.key());
            case ARGUMENT -> argument(spec, part.key());
        };
    }
}
