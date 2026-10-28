package io.github.lukmnh.toolbox.devutils.encoder.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser URL manual. {@link java.net.URI} tidak dipakai karena menolak URL "kotor" yang sering di-paste (spasi,
 * karakter non-ASCII), padahal tool ini justru untuk memeriksanya.
 */
public final class UrlParser {

    private static final int MAX_PORT = 65_535;
    private static final int MAX_PORT_DIGITS = 5;

    private UrlParser() {}

    /** @throws InvalidPercentEncodingException bila query berisi {@code %} rusak; posisi relatif ke {@code raw} */
    public static ParsedUrl parse(String raw) {
        int offset = raw.length() - raw.stripLeading().length();
        String url = raw.strip();

        int fragmentStart = url.indexOf('#');
        boolean hasFragment = fragmentStart >= 0;
        String fragment = hasFragment ? url.substring(fragmentStart + 1) : "";
        String rest = hasFragment ? url.substring(0, fragmentStart) : url;

        int queryStart = rest.indexOf('?');
        List<QueryParam> params = queryStart < 0 ? List.of() : parseQuery(rest, queryStart + 1, offset);
        rest = queryStart < 0 ? rest : rest.substring(0, queryStart);

        int schemeEnd = schemeEnd(rest);
        String scheme = schemeEnd < 0 ? "" : rest.substring(0, schemeEnd);
        rest = rest.substring(schemeEnd + 1);

        boolean hasAuthority = rest.startsWith("//");
        String authority = "";
        if (hasAuthority) {
            rest = rest.substring(2);
            int pathStart = rest.indexOf('/');
            authority = pathStart < 0 ? rest : rest.substring(0, pathStart);
            rest = pathStart < 0 ? "" : rest.substring(pathStart);
        }
        String hostPort = authority.substring(authority.lastIndexOf('@') + 1);
        return new ParsedUrl(
                scheme, hasAuthority, authority, host(hostPort), port(hostPort), rest, params, hasFragment, fragment);
    }

    /** Indeks {@code :} penutup scheme ({@code ALPHA *( ALPHA / DIGIT / + / - / . )}), atau -1. */
    private static int schemeEnd(String text) {
        int colon = text.indexOf(':');
        if (colon < 1 || !Character.isLetter(text.charAt(0))) {
            return -1;
        }
        for (int index = 1; index < colon; index++) {
            char character = text.charAt(index);
            boolean valid =
                    Character.isLetterOrDigit(character) || character == '+' || character == '-' || character == '.';
            if (!valid || character > 0x7F) {
                return -1;
            }
        }
        return colon;
    }

    private static List<QueryParam> parseQuery(String text, int start, int offset) {
        List<QueryParam> params = new ArrayList<>();
        int segmentStart = start;
        while (segmentStart <= text.length()) {
            int ampersand = text.indexOf('&', segmentStart);
            int segmentEnd = ampersand < 0 ? text.length() : ampersand;
            if (segmentEnd > segmentStart) {
                params.add(param(text, segmentStart, segmentEnd, offset));
            }
            segmentStart = segmentEnd + 1;
        }
        return params;
    }

    private static QueryParam param(String text, int start, int end, int offset) {
        int equals = text.indexOf('=', start);
        boolean hasValue = equals >= 0 && equals < end;
        int keyEnd = hasValue ? equals : end;
        String key = decode(text, start, keyEnd, offset);
        String value = hasValue ? decode(text, equals + 1, end, offset) : "";
        return new QueryParam(key, value);
    }

    private static String decode(String text, int start, int end, int offset) {
        try {
            return UrlCodec.decodeComponent(text.substring(start, end), true);
        } catch (InvalidPercentEncodingException e) {
            throw new InvalidPercentEncodingException(offset + start + e.position(), e.sequence());
        }
    }

    private static String host(String hostPort) {
        if (hostPort.startsWith("[")) {
            int close = hostPort.indexOf(']');
            return close < 0 ? hostPort : hostPort.substring(0, close + 1);
        }
        int colon = hostPort.lastIndexOf(':');
        return colon < 0 ? hostPort : hostPort.substring(0, colon);
    }

    private static int port(String hostPort) {
        int colon = hostPort.startsWith("[")
                ? hostPort.indexOf(':', Math.max(0, hostPort.indexOf(']')))
                : hostPort.lastIndexOf(':');
        if (colon < 0) {
            return ParsedUrl.NO_PORT;
        }
        String digits = hostPort.substring(colon + 1);
        if (digits.isEmpty()
                || digits.length() > MAX_PORT_DIGITS
                || !digits.chars().allMatch(c -> c >= '0' && c <= '9')) {
            return ParsedUrl.NO_PORT;
        }
        int port = Integer.parseInt(digits);
        return port <= MAX_PORT ? port : ParsedUrl.NO_PORT;
    }
}
