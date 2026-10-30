package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.io.Serial;
import java.util.List;

/** Pola JavaScript/PHP tidak bisa dijalankan: ada error sintaks atau fitur tanpa padanan di engine terjemahan. */
public final class UnsupportedRegexException extends RegexToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient List<RegexProblem> problems;

    public UnsupportedRegexException(List<RegexProblem> problems) {
        super("Pattern cannot run: " + problems.size() + " problem(s)", null);
        if (problems.isEmpty()) {
            throw new IllegalArgumentException("At least one problem is required");
        }
        this.problems = List.copyOf(problems);
    }

    public List<RegexProblem> problems() {
        return problems;
    }
}
