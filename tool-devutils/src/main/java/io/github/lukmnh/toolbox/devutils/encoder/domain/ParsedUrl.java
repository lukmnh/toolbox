package io.github.lukmnh.toolbox.devutils.encoder.domain;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * URL yang sudah dipecah. Bagian selain query disimpan mentah (tidak di-decode) supaya URL bisa dibangun ulang
 * persis; hanya parameter query yang bisa diedit.
 *
 * @param scheme kosong bila tidak ada
 * @param hasAuthority {@code true} bila ada {@code //}, termasuk authority kosong seperti {@code file:///}
 * @param port {@link #NO_PORT} bila tidak ada
 */
public record ParsedUrl(
        String scheme,
        boolean hasAuthority,
        String authority,
        String host,
        int port,
        String path,
        List<QueryParam> params,
        boolean hasFragment,
        String fragment) {

    public static final int NO_PORT = -1;

    public ParsedUrl {
        Objects.requireNonNull(scheme, "scheme");
        Objects.requireNonNull(authority, "authority");
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(fragment, "fragment");
        params = List.copyOf(params);
    }

    public boolean hasPort() {
        return port != NO_PORT;
    }

    /** Bangun ulang URL dengan parameter baru; key/value di-encode sebagai komponen. */
    public String toUrl(List<QueryParam> newParams, SpaceEncoding spaces) {
        StringBuilder url = new StringBuilder();
        if (!scheme.isEmpty()) {
            url.append(scheme).append(':');
        }
        if (hasAuthority) {
            url.append("//").append(authority);
        }
        url.append(path);
        if (!newParams.isEmpty()) {
            url.append('?').append(query(newParams, spaces));
        }
        if (hasFragment) {
            url.append('#').append(fragment);
        }
        return url.toString();
    }

    private static String query(List<QueryParam> params, SpaceEncoding spaces) {
        return params.stream()
                .map(param -> UrlCodec.encodeComponent(param.key(), spaces) + "="
                        + UrlCodec.encodeComponent(param.value(), spaces))
                .collect(Collectors.joining("&"));
    }
}
