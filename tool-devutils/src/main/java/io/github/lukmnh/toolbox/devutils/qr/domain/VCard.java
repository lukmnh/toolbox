package io.github.lukmnh.toolbox.devutils.qr.domain;

import java.util.Objects;

/**
 * Kartu kontak vCard 3.0 (RFC 2426) untuk QR. Semua field di-trim; field kosong tidak ditulis.
 *
 * @param name nama lengkap; wajib (vCard 3.0 mensyaratkan {@code N} dan {@code FN})
 */
public record VCard(
        String name, String organization, String title, String phone, String email, String website, String address) {

    private static final String CRLF = "\r\n";

    public VCard {
        name = clean(name);
        organization = clean(organization);
        title = clean(title);
        phone = clean(phone);
        email = clean(email);
        website = clean(website);
        address = clean(address);
    }

    public boolean hasName() {
        return !name.isEmpty();
    }

    /**
     * Teks vCard dengan baris CRLF. Nama lengkap ditaruh di komponen family name {@code N} supaya tidak menebak
     * pemisahan nama depan/belakang.
     *
     * @throws IllegalStateException bila nama kosong; cek {@link #hasName()} dulu
     */
    public String toPayload() {
        if (!hasName()) {
            throw new IllegalStateException("vCard name is empty");
        }
        StringBuilder card = new StringBuilder("BEGIN:VCARD")
                .append(CRLF)
                .append("VERSION:3.0")
                .append(CRLF);
        line(card, "N", escape(name) + ";;;;");
        line(card, "FN", escape(name));
        optional(card, "ORG", organization);
        optional(card, "TITLE", title);
        optional(card, "TEL;TYPE=VOICE", phone);
        optional(card, "EMAIL;TYPE=INTERNET", email);
        optional(card, "URL", website);
        if (!address.isEmpty()) {
            // ADR: PO box; extended; street; locality; region; postal code; country
            line(card, "ADR", ";;" + escape(address) + ";;;;");
        }
        return card.append("END:VCARD").append(CRLF).toString();
    }

    /** Escape nilai TEXT RFC 2426 §4: {@code \ , ;} diberi backslash, baris baru menjadi {@code \n}. */
    static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            switch (current) {
                case '\\' -> escaped.append("\\\\");
                case ',' -> escaped.append("\\,");
                case ';' -> escaped.append("\\;");
                case '\n' -> escaped.append("\\n");
                default -> escaped.append(current);
            }
        }
        return escaped.toString();
    }

    private static void optional(StringBuilder card, String property, String value) {
        if (!value.isEmpty()) {
            line(card, property, escape(value));
        }
    }

    private static void line(StringBuilder card, String property, String value) {
        card.append(property).append(':').append(value).append(CRLF);
    }

    private static String clean(String value) {
        return Objects.requireNonNullElse(value, "").strip();
    }
}
