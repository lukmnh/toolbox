package io.github.lukmnh.toolbox.devutils.hash.domain;

import java.util.Base64;

/** Encoding PEM (RFC 7468): Base64 standar, dipotong 64 kolom, diapit baris BEGIN/END. */
public final class Pem {

    public static final String PUBLIC_KEY = "PUBLIC KEY";
    public static final String PRIVATE_KEY = "PRIVATE KEY";

    static final int LINE_LENGTH = 64;

    private Pem() {}

    /** @param label {@link #PUBLIC_KEY} untuk DER SPKI atau {@link #PRIVATE_KEY} untuk DER PKCS#8 */
    public static String encode(String label, byte[] der) {
        String body = Base64.getEncoder().encodeToString(der);
        StringBuilder pem = new StringBuilder(body.length() + body.length() / LINE_LENGTH + 2 * label.length() + 40);
        pem.append("-----BEGIN ").append(label).append("-----\n");
        for (int start = 0; start < body.length(); start += LINE_LENGTH) {
            pem.append(body, start, Math.min(body.length(), start + LINE_LENGTH))
                    .append('\n');
        }
        return pem.append("-----END ").append(label).append("-----\n").toString();
    }
}
