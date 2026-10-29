package io.github.lukmnh.toolbox.devutils.regex.application;

import io.github.lukmnh.toolbox.devutils.regex.domain.MatchScan;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexRequest;
import io.github.lukmnh.toolbox.devutils.regex.domain.ScanLimits;
import java.time.Duration;

/** Jalankan pola pada teks uji: detail 1.000 match pertama + jumlah total. */
public final class RunRegexUseCase {

    /** Batas detail match untuk highlight dan tabel (spec #12). */
    public static final int MAX_DETAILED_MATCHES = 1_000;

    /** Penghitungan berhenti di sini supaya pola yang cocok di setiap posisi tidak menghabiskan waktu. */
    public static final int MAX_COUNTED_MATCHES = 1_000_000;

    private final RegexCompiler compiler;

    public RunRegexUseCase(RegexCompiler compiler) {
        this.compiler = compiler;
    }

    public RegexRun execute(RegexRequest request, String text, Duration timeout) {
        RegexCompiler.Prepared prepared = compiler.prepare(request);
        boolean firstOnly = prepared.pattern().firstMatchOnly();
        ScanLimits limits = firstOnly
                ? new ScanLimits(1, 1, timeout)
                : new ScanLimits(MAX_DETAILED_MATCHES, MAX_COUNTED_MATCHES, timeout);
        MatchScan scan = prepared.compiled().scan(text, limits);
        return new RegexRun(scan, prepared.pattern().groupNames());
    }
}
