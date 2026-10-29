package io.github.lukmnh.toolbox.devutils.regex.infrastructure;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;
import io.github.lukmnh.toolbox.common.text.InterruptibleCharSequence;
import io.github.lukmnh.toolbox.common.text.TimedRegex;
import io.github.lukmnh.toolbox.devutils.regex.domain.CompiledRegex;
import io.github.lukmnh.toolbox.devutils.regex.domain.EngineKind;
import io.github.lukmnh.toolbox.devutils.regex.domain.EnginePattern;
import io.github.lukmnh.toolbox.devutils.regex.domain.MatchScan;
import io.github.lukmnh.toolbox.devutils.regex.domain.OffsetMap;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexCompileException;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexEngine;
import io.github.lukmnh.toolbox.devutils.regex.domain.ScanLimits;
import java.util.List;

/**
 * Adapter RE2/J untuk flavor Go. Iterasi match meniru {@code regexp.FindAll} Go persis: match kosong yang menempel
 * tepat setelah match sebelumnya diabaikan (RE2/J {@code find()} sendiri tidak melakukannya).
 */
public final class Re2jRegexEngine implements RegexEngine {

    /** Format pesan RE2/J (sama dengan Go): {@code error parsing regexp: <deskripsi>: `<fragmen>`}. */
    private static final String MESSAGE_PREFIX = "error parsing regexp: ";

    private static final String FRAGMENT_OPEN = ": `";
    private static final char FRAGMENT_CLOSE = '`';

    @Override
    public EngineKind kind() {
        return EngineKind.RE2;
    }

    @Override
    public CompiledRegex compile(EnginePattern pattern) {
        try {
            return new Compiled(Pattern.compile(pattern.source(), pattern.flags()), pattern.groupNames());
        } catch (PatternSyntaxException e) {
            throw new RegexCompileException(e.getDescription(), position(e, pattern.source(), pattern.offsets()), e);
        }
    }

    /** RE2/J tidak mengisi indeks error; posisi dicari dari fragmen yang dikutip di pesan. */
    static int position(PatternSyntaxException error, String source, OffsetMap offsets) {
        String message = error.getMessage();
        String head = MESSAGE_PREFIX + error.getDescription() + FRAGMENT_OPEN;
        if (message == null || !message.startsWith(head) || message.charAt(message.length() - 1) != FRAGMENT_CLOSE) {
            return RegexCompileException.UNKNOWN_POSITION;
        }
        String fragment = message.substring(head.length(), message.length() - 1);
        int index = fragment.isEmpty() ? -1 : source.indexOf(fragment);
        return index < 0 ? RegexCompileException.UNKNOWN_POSITION : offsets.toOriginal(index);
    }

    private record Compiled(Pattern pattern, List<String> groupNames) implements CompiledRegex {

        @Override
        public int groupCount() {
            return pattern.groupCount();
        }

        @Override
        public MatchScan scan(CharSequence text, ScanLimits limits) {
            String original = text.toString();
            return TimedRegex.run(original, limits.timeout(), sequence -> collect(original, sequence, limits));
        }

        /** Salinan logika {@code Regexp.allMatches} Go dengan indeks UTF-16 (bukan byte UTF-8). */
        private MatchScan collect(String original, InterruptibleCharSequence sequence, ScanLimits limits) {
            Matcher matcher = pattern.matcher(sequence);
            MatchCollector collector = new MatchCollector(original, groupNames, pattern.groupCount(), limits);
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
            int position = 0;
            int previousEnd = -1;
            while (position <= original.length() && matcher.find(position)) {
                boolean accept = true;
                int end = matcher.end();
                if (end == position) {
                    // Match kosong tepat setelah match sebelumnya tidak dihitung (aturan Go)
                    accept = matcher.start() != previousEnd;
                    position += runeWidth(original, position);
                } else {
                    position = end;
                }
                previousEnd = end;
                if (accept) {
                    if (collector.isFull()) {
                        return collector.result(false);
                    }
                    collector.add(bounds);
                }
                sequence.checkBudget();
            }
            return collector.result(true);
        }

        private static int runeWidth(String text, int position) {
            return position < text.length() ? Character.charCount(text.codePointAt(position)) : 1;
        }
    }
}
