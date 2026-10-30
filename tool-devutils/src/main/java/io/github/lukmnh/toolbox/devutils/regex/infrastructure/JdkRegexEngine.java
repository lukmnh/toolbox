package io.github.lukmnh.toolbox.devutils.regex.infrastructure;

import io.github.lukmnh.toolbox.common.text.InterruptibleCharSequence;
import io.github.lukmnh.toolbox.common.text.TimedRegex;
import io.github.lukmnh.toolbox.devutils.regex.domain.CompiledRegex;
import io.github.lukmnh.toolbox.devutils.regex.domain.EngineKind;
import io.github.lukmnh.toolbox.devutils.regex.domain.EnginePattern;
import io.github.lukmnh.toolbox.devutils.regex.domain.MatchScan;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexCompileException;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexEngine;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexTooComplexException;
import io.github.lukmnh.toolbox.devutils.regex.domain.ScanLimits;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Adapter {@code java.util.regex}: flavor Java asli, juga pola JavaScript/PHP hasil terjemahan. */
public final class JdkRegexEngine implements RegexEngine {

    @Override
    public EngineKind kind() {
        return EngineKind.JDK;
    }

    @Override
    public CompiledRegex compile(EnginePattern pattern) {
        try {
            return new Compiled(Pattern.compile(pattern.source(), pattern.flags()), pattern.groupNames());
        } catch (PatternSyntaxException e) {
            int position = e.getIndex() < 0
                    ? RegexCompileException.UNKNOWN_POSITION
                    : pattern.offsets().toOriginal(e.getIndex());
            throw new RegexCompileException(e.getDescription(), position, e);
        } catch (IllegalArgumentException e) {
            // Kombinasi flag tidak dikenal; bukan kesalahan posisi tertentu di pola
            throw new RegexCompileException(e.getMessage(), RegexCompileException.UNKNOWN_POSITION, e);
        } catch (StackOverflowError e) {
            throw new RegexTooComplexException(e);
        }
    }

    private record Compiled(Pattern pattern, List<String> groupNames) implements CompiledRegex {

        @Override
        public int groupCount() {
            return pattern.matcher("").groupCount();
        }

        @Override
        public MatchScan scan(CharSequence text, ScanLimits limits) {
            String original = text.toString();
            try {
                return TimedRegex.run(original, limits.timeout(), sequence -> collect(original, sequence, limits));
            } catch (StackOverflowError e) {
                throw new RegexTooComplexException(e);
            }
        }

        private MatchScan collect(String original, InterruptibleCharSequence sequence, ScanLimits limits) {
            Matcher matcher = pattern.matcher(sequence);
            MatchCollector collector = new MatchCollector(original, groupNames, matcher.groupCount(), limits);
            GroupBounds bounds = new GroupBounds() {
                @Override
                public int start(int group) {
                    return matcher.start(group);
                }

                @Override
                public int end(int group) {
                    return matcher.end(group);
                }
            };
            while (!collector.isFull()) {
                if (!matcher.find()) {
                    return collector.result(true);
                }
                collector.add(bounds);
                sequence.checkBudget();
            }
            // Batas hitungan tercapai: hitungan lengkap hanya bila memang tidak ada match lagi
            return collector.result(!matcher.find());
        }
    }
}
