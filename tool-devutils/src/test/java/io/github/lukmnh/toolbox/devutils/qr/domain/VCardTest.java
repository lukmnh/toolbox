package io.github.lukmnh.toolbox.devutils.qr.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VCardTest {

    @Test
    void buildsVCard30WithCrlfAndAllFields() {
        VCard card = new VCard(
                " Budi Santoso ",
                "PT Maju",
                "Engineer",
                "+62 812 0000",
                "budi@example.com",
                "https://example.com",
                "Jl. Merdeka 1");

        assertEquals(
                "BEGIN:VCARD\r\n"
                        + "VERSION:3.0\r\n"
                        + "N:Budi Santoso;;;;\r\n"
                        + "FN:Budi Santoso\r\n"
                        + "ORG:PT Maju\r\n"
                        + "TITLE:Engineer\r\n"
                        + "TEL;TYPE=VOICE:+62 812 0000\r\n"
                        + "EMAIL;TYPE=INTERNET:budi@example.com\r\n"
                        + "URL:https://example.com\r\n"
                        + "ADR:;;Jl. Merdeka 1;;;;\r\n"
                        + "END:VCARD\r\n",
                card.toPayload());
    }

    @Test
    void omitsEmptyOptionalFields() {
        VCard card = new VCard("Ana", "", null, " ", "", "", "");

        assertEquals("BEGIN:VCARD\r\nVERSION:3.0\r\nN:Ana;;;;\r\nFN:Ana\r\nEND:VCARD\r\n", card.toPayload());
    }

    @Test
    void escapesSpecialCharactersPerRfc2426() {
        assertEquals("a\\\\b\\,c\\;d\\ne\\nf\\ng", VCard.escape("a\\b,c;d\ne\r\nf\rg"));

        VCard card = new VCard("Doe, John", "R&D; Lab", "", "", "", "", "Line 1\nCity, 123");
        String payload = card.toPayload();
        assertTrue(payload.contains("FN:Doe\\, John\r\n"));
        assertTrue(payload.contains("ORG:R&D\\; Lab\r\n"));
        assertTrue(payload.contains("ADR:;;Line 1\\nCity\\, 123;;;;\r\n"));
    }

    @Test
    void requiresName() {
        VCard card = new VCard("  ", "PT Maju", "", "", "", "", "");

        assertFalse(card.hasName());
        assertThrows(IllegalStateException.class, card::toPayload);
    }
}
