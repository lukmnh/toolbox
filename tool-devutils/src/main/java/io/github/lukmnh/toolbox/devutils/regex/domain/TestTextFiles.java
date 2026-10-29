package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.nio.file.Path;

/** Port baca file teks uji ({@code .txt}/{@code .log}). */
public interface TestTextFiles {

    /**
     * @throws TestTextTooLargeException bila melebihi batas
     * @throws RegexIoException bila gagal dibaca
     */
    LoadedTestText read(Path file);
}
