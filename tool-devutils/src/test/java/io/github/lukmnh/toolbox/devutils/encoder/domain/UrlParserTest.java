package io.github.lukmnh.toolbox.devutils.encoder.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class UrlParserTest {

    @Test
    void splitsAllParts() {
        ParsedUrl url = UrlParser.parse("https://user@api.contoh.id:8443/v1/items?page=2#hasil");

        assertEquals("https", url.scheme());
        assertEquals("user@api.contoh.id:8443", url.authority());
        assertEquals("api.contoh.id", url.host());
        assertEquals(8443, url.port());
        assertEquals("/v1/items", url.path());
        assertEquals(List.of(new QueryParam("page", "2")), url.params());
        assertEquals("hasil", url.fragment());
    }

    @Test
    void ipv6HostAndMissingPort() {
        ParsedUrl withPort = UrlParser.parse("http://[::1]:8080/");
        ParsedUrl withoutPort = UrlParser.parse("http://[::1]/");

        assertEquals("[::1]", withPort.host());
        assertEquals(8080, withPort.port());
        assertFalse(withoutPort.hasPort());
    }

    @Test
    void duplicateKeysEmptyValuesAndUnicodeAreKept() {
        ParsedUrl url = UrlParser.parse("https://x.id/?tag=a&tag=b&kosong=&flag&q=caf%C3%A9+susu&&");

        assertEquals(
                List.of(
                        new QueryParam("tag", "a"),
                        new QueryParam("tag", "b"),
                        new QueryParam("kosong", ""),
                        new QueryParam("flag", ""),
                        new QueryParam("q", "café susu")),
                url.params());
    }

    @Test
    void rebuildsWithEditedParams() {
        ParsedUrl url = UrlParser.parse("https://x.id/cari?q=lama#atas");

        String rebuilt = url.toUrl(
                List.of(new QueryParam("q", "kopi susu"), new QueryParam("kota", "Bandung")), SpaceEncoding.PLUS);

        assertEquals("https://x.id/cari?q=kopi+susu&kota=Bandung#atas", rebuilt);
        assertEquals("https://x.id/cari#atas", url.toUrl(List.of(), SpaceEncoding.PLUS));
    }

    @Test
    void roundTripWithoutEditsKeepsUrl() {
        String original = "https://x.id/a/b?x=1&y=caf%C3%A9#f";
        ParsedUrl url = UrlParser.parse(original);

        assertEquals(original, url.toUrl(url.params(), SpaceEncoding.PERCENT_20));
    }

    @Test
    void urlWithoutSchemeIsTreatedAsPath() {
        ParsedUrl url = UrlParser.parse("/api/v1?x=1");

        assertEquals("", url.scheme());
        assertFalse(url.hasAuthority());
        assertEquals("/api/v1", url.path());
        assertTrue(url.toUrl(url.params(), SpaceEncoding.PLUS).startsWith("/api/v1?x=1"));
    }

    @Test
    void brokenPercentInQueryReportsPositionInWholeUrl() {
        InvalidPercentEncodingException error =
                assertThrows(InvalidPercentEncodingException.class, () -> UrlParser.parse("  https://x.id/?a=1&b=%ZZ"));

        assertEquals(22, error.position());
    }
}
