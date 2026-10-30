package io.github.lukmnh.toolbox.devutils.regex.application;

import io.github.lukmnh.toolbox.devutils.regex.domain.CompiledRegex;
import io.github.lukmnh.toolbox.devutils.regex.domain.EngineKind;
import io.github.lukmnh.toolbox.devutils.regex.domain.EnginePattern;
import io.github.lukmnh.toolbox.devutils.regex.domain.EnginePatternFactory;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexEngine;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexParser;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexRequest;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Pola → {@link EnginePattern} (terjemahan bila perlu) → kompilasi di engine yang sesuai flavor. */
public final class RegexCompiler {

    private final Map<EngineKind, RegexEngine> engines = new EnumMap<>(EngineKind.class);

    /** @param engines satu engine per {@link EngineKind} */
    public RegexCompiler(List<RegexEngine> engines) {
        engines.forEach(engine -> this.engines.put(engine.kind(), engine));
        for (EngineKind kind : EngineKind.values()) {
            if (!this.engines.containsKey(kind)) {
                throw new IllegalArgumentException("Engine " + kind + " is not wired");
            }
        }
    }

    /**
     * @throws io.github.lukmnh.toolbox.devutils.regex.domain.UnsupportedRegexException pola JS/PHP tidak bisa
     *     diterjemahkan
     * @throws io.github.lukmnh.toolbox.devutils.regex.domain.RegexCompileException engine menolak pola
     */
    public Prepared prepare(RegexRequest request) {
        EnginePattern pattern = EnginePatternFactory.create(
                RegexParser.parse(request.pattern(), request.flavor(), request.flags()), request);
        CompiledRegex compiled = engines.get(pattern.engine()).compile(pattern);
        return new Prepared(pattern, compiled);
    }

    public record Prepared(EnginePattern pattern, CompiledRegex compiled) {}
}
