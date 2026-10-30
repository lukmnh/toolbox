package io.github.lukmnh.toolbox.devutils.git.domain;

import java.util.Map;
import java.util.Set;

/** Input builder: nilai argumen, opsi flag yang dicentang, dan nilai opsi ber-value (kosong = tidak dipakai). */
public record GitRequest(
        GitCommandSpec command, Map<String, String> arguments, Set<String> flags, Map<String, String> values) {

    public GitRequest {
        arguments = Map.copyOf(arguments);
        flags = Set.copyOf(flags);
        values = Map.copyOf(values);
    }
}
