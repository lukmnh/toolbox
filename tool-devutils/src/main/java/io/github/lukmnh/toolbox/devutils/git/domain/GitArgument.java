package io.github.lukmnh.toolbox.devutils.git.domain;

/** Argumen posisi (ditulis setelah opsi), misalnya nama branch. */
public record GitArgument(String key, boolean required) {

    public static GitArgument required(String key) {
        return new GitArgument(key, true);
    }

    public static GitArgument optional(String key) {
        return new GitArgument(key, false);
    }
}
