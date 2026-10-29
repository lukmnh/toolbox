package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Pola + flavor + flag yang diminta user. */
public record RegexRequest(String pattern, RegexFlavor flavor, Set<RegexFlag> flags) {

    public RegexRequest {
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(flavor, "flavor");
        flags = flags.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(flags));
        for (RegexFlag flag : flags) {
            if (flag.flavor() != flavor) {
                throw new IllegalArgumentException("Flag " + flag + " bukan milik " + flavor);
            }
        }
    }

    public boolean has(RegexFlag flag) {
        return flags.contains(flag);
    }
}
