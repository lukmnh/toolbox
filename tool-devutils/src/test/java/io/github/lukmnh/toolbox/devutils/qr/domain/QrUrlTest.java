package io.github.lukmnh.toolbox.devutils.qr.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class QrUrlTest {

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com/promo?code=A1", "HTTP://example.com", "http://127.0.0.1:8080/x"})
    void acceptsHttpAndHttps(String url) {
        assertEquals(url, QrUrl.validate("  " + url + "\n"));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"example.com", "ftp://example.com", "javascript:alert(1)", "https://", "https://exa mple.com"})
    void rejectsNonWebUrls(String url) {
        assertThrows(InvalidQrUrlException.class, () -> QrUrl.validate(url));
    }
}
