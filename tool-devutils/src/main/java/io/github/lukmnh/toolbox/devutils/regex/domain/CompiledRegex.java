package io.github.lukmnh.toolbox.devutils.regex.domain;

/** Pola yang sudah dikompilasi engine. */
public interface CompiledRegex {

    int groupCount();

    /**
     * Cari match di {@code text}.
     *
     * @throws io.github.lukmnh.toolbox.common.text.RegexTimeoutException bila melewati {@link ScanLimits#timeout()}
     * @throws io.github.lukmnh.toolbox.common.task.TaskCancelledException bila thread di-interrupt
     */
    MatchScan scan(CharSequence text, ScanLimits limits);
}
