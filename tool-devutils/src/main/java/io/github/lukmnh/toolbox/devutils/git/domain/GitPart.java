package io.github.lukmnh.toolbox.devutils.git.domain;

/** Satu token hasil build beserta asalnya, untuk ditampilkan berdampingan dengan penjelasannya. */
public record GitPart(String text, Kind kind, String key, boolean dangerous) {

    public enum Kind {
        COMMAND,
        OPTION,
        ARGUMENT
    }
}
