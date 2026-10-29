package io.github.lukmnh.toolbox.devutils.regex.presentation;

import io.github.lukmnh.toolbox.devutils.regex.application.RegexAnalysis;
import io.github.lukmnh.toolbox.devutils.regex.application.RegexRun;
import io.github.lukmnh.toolbox.devutils.regex.application.ReplaceResult;
import java.util.Objects;
import java.util.Optional;

/**
 * Hasil satu evaluasi live. Run dan replace bisa gagal sendiri-sendiri (misalnya pola valid tapi template
 * replacement salah), jadi kegagalan disimpan per bagian untuk ditampilkan inline, bukan dialog.
 */
record Evaluation(
        String text,
        RegexAnalysis analysis,
        Optional<RegexRun> run,
        Optional<RuntimeException> runFailure,
        Optional<ReplaceResult> replace,
        Optional<RuntimeException> replaceFailure) {

    Evaluation {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(analysis, "analysis");
        Objects.requireNonNull(run, "run");
        Objects.requireNonNull(runFailure, "runFailure");
        Objects.requireNonNull(replace, "replace");
        Objects.requireNonNull(replaceFailure, "replaceFailure");
    }
}
