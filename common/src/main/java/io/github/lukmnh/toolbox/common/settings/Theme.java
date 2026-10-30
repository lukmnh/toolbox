package io.github.lukmnh.toolbox.common.settings;

/** Tema UI. {@link #LIGHT} adalah default bila user belum pernah memilih. */
public enum Theme {
    LIGHT,
    DARK;

    public Theme toggled() {
        return this == DARK ? LIGHT : DARK;
    }
}
