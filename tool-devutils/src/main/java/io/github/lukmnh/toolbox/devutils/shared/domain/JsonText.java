package io.github.lukmnh.toolbox.devutils.shared.domain;

/** Literal string JSON (juga valid sebagai string JavaScript/mongosh). Hanya JDK supaya bisa dipakai di domain. */
public final class JsonText {

    private JsonText() {}

    /** Bungkus dengan kutip ganda; kutip, backslash, dan karakter kontrol di-escape. */
    public static String quote(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
