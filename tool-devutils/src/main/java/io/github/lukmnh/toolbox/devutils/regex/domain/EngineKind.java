package io.github.lukmnh.toolbox.devutils.regex.domain;

/** Engine yang menjalankan pola. */
public enum EngineKind {
    /** {@code java.util.regex}: Java asli, juga hasil terjemahan JavaScript dan PHP. */
    JDK,
    /** RE2/J: port resmi RE2, sintaks dan semantik sama dengan paket {@code regexp} Go. */
    RE2
}
