package io.github.lukmnh.toolbox.productivity.rename.infrastructure;

import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.common.text.RegexTimeoutException;
import io.github.lukmnh.toolbox.common.text.TimedRegex;
import io.github.lukmnh.toolbox.productivity.rename.domain.InvalidRuleException;
import io.github.lukmnh.toolbox.productivity.rename.domain.PatternReplacer;
import java.util.regex.Pattern;

/** {@link PatternReplacer} lewat {@link TimedRegex}: regex yang backtracking berlebihan dihentikan, bukan hang. */
public final class TimedPatternReplacer implements PatternReplacer {

    @Override
    public String replaceAll(Pattern pattern, String text, String replacement) {
        try {
            return TimedRegex.run(
                    text,
                    SecurityLimits.REGEX_TIMEOUT,
                    sequence -> pattern.matcher(sequence).replaceAll(replacement));
        } catch (RegexTimeoutException e) {
            throw new InvalidRuleException(InvalidRuleException.Problem.REGEX_TIMEOUT, e);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            // Teks pengganti merujuk grup yang tidak ada ($3, ${nama}) atau diakhiri \ / $ yatim
            throw new InvalidRuleException(InvalidRuleException.Problem.INVALID_REPLACEMENT, e);
        }
    }
}
