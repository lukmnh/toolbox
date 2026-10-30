package io.github.lukmnh.toolbox.devutils.regex.domain;

/** Port ke engine regex ({@code java.util.regex} atau RE2/J). */
public interface RegexEngine {

    EngineKind kind();

    /** @throws RegexCompileException dengan posisi yang sudah dipetakan ke pola asli lewat {@link EnginePattern#offsets()} */
    CompiledRegex compile(EnginePattern pattern);
}
