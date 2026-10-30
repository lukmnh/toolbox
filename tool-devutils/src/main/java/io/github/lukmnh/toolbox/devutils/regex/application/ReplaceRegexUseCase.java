package io.github.lukmnh.toolbox.devutils.regex.application;

import io.github.lukmnh.toolbox.devutils.regex.domain.MatchScan;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexRequest;
import io.github.lukmnh.toolbox.devutils.regex.domain.ReplacementTemplate;
import io.github.lukmnh.toolbox.devutils.regex.domain.ScanLimits;
import java.time.Duration;

/**
 * Replace dengan sintaks replacement flavor terkait (Java {@code $1 ${n}}, JS {@code $& $<n>}, Go {@code ${1}
 * $name}, PHP {@code $1 \\1}). JavaScript tanpa flag {@code g} hanya mengganti match pertama.
 */
public final class ReplaceRegexUseCase {

    /** Batas jumlah penggantian; teks uji maks 5 MB sehingga batas ini praktis tidak tercapai. */
    public static final int MAX_REPLACEMENTS = 1_000_000;

    private final RegexCompiler compiler;

    public ReplaceRegexUseCase(RegexCompiler compiler) {
        this.compiler = compiler;
    }

    /** @throws io.github.lukmnh.toolbox.devutils.regex.domain.InvalidReplacementException template tidak valid */
    public ReplaceResult execute(RegexRequest request, String text, String replacement, Duration timeout) {
        RegexCompiler.Prepared prepared = compiler.prepare(request);
        ReplacementTemplate template = ReplacementTemplate.parse(
                replacement, request.flavor(), prepared.pattern().groupNames());
        int limit = prepared.pattern().firstMatchOnly() ? 1 : MAX_REPLACEMENTS;
        MatchScan scan = prepared.compiled().scan(text, new ScanLimits(limit, limit, timeout));
        return new ReplaceResult(
                template.replaceAll(text, scan.matches()), scan.matches().size());
    }
}
