package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.Arrays;
import java.util.List;

/**
 * Dialek regex yang didukung. Java dan Go dijalankan engine aslinya; JavaScript dan PHP (PCRE) di-parse sendiri,
 * divalidasi per fitur, lalu diterjemahkan ke sintaks {@code java.util.regex} dengan koreksi semantik.
 */
public enum RegexFlavor {
    JAVA(EngineKind.JDK, false),
    JAVASCRIPT(EngineKind.JDK, true),
    GO(EngineKind.RE2, false),
    PHP(EngineKind.JDK, true);

    private final EngineKind engine;
    private final boolean translated;

    RegexFlavor(EngineKind engine, boolean translated) {
        this.engine = engine;
        this.translated = translated;
    }

    public EngineKind engine() {
        return engine;
    }

    /** {@code true} bila pola diterjemahkan ke engine lain (hasil "sangat dekat", bukan engine asli). */
    public boolean translated() {
        return translated;
    }

    public List<RegexFlag> flags() {
        return Arrays.stream(RegexFlag.values())
                .filter(flag -> flag.flavor() == this)
                .toList();
    }
}
