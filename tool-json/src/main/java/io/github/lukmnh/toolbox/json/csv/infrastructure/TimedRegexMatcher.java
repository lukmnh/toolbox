package io.github.lukmnh.toolbox.json.csv.infrastructure;

import io.github.lukmnh.toolbox.common.text.RegexTimeoutException;
import io.github.lukmnh.toolbox.common.text.TimedRegex;
import io.github.lukmnh.toolbox.json.csv.domain.RegexSplitException;
import io.github.lukmnh.toolbox.json.csv.domain.TimedMatcher;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Adapter {@link TimedRegex}: setiap nilai sel punya jatah waktu sendiri. */
public final class TimedRegexMatcher implements TimedMatcher {

    private final Duration timeout;

    public TimedRegexMatcher(Duration timeout) {
        this.timeout = timeout;
    }

    @Override
    public List<String> captureGroups(Pattern pattern, String value) {
        try {
            return TimedRegex.run(value, timeout, sequence -> groups(pattern.matcher(sequence)));
        } catch (RegexTimeoutException e) {
            throw new RegexSplitException(RegexSplitException.Reason.TIMEOUT, timeout, e);
        } catch (StackOverflowError e) {
            // Pola dengan rekursi dalam (misalnya alternasi panjang) bisa menghabiskan stack sebelum timeout
            throw new RegexSplitException(RegexSplitException.Reason.TOO_COMPLEX, timeout, e);
        }
    }

    private static List<String> groups(Matcher matcher) {
        if (!matcher.find()) {
            return List.of();
        }
        List<String> groups = new ArrayList<>(matcher.groupCount());
        for (int group = 1; group <= matcher.groupCount(); group++) {
            groups.add(Objects.requireNonNullElse(matcher.group(group), ""));
        }
        return groups;
    }
}
