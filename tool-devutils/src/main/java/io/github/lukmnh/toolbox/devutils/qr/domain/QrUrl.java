package io.github.lukmnh.toolbox.devutils.qr.domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Validasi URL untuk QR promo: hanya http/https absolut dengan host, supaya scanner membukanya di browser. */
public final class QrUrl {

    private QrUrl() {}

    /**
     * @return URL yang sudah di-trim, siap di-encode
     * @throws InvalidQrUrlException bila bukan URL http/https yang valid
     */
    public static String validate(String raw) {
        String url = raw.strip();
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw new InvalidQrUrlException(null);
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new InvalidQrUrlException(null);
            }
            return url;
        } catch (URISyntaxException e) {
            throw new InvalidQrUrlException(e);
        }
    }
}
