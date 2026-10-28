package io.github.lukmnh.toolbox.devutils.encoder.domain;

import java.util.Locale;
import java.util.Map;

/** Fallback MIME type dari ekstensi, untuk OS yang tidak bisa mendeteksi tipe file (data URI). */
public final class MimeTypes {

    public static final String DEFAULT = "application/octet-stream";

    private static final Map<String, String> BY_EXTENSION = Map.ofEntries(
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("ico", "image/x-icon"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("zip", "application/zip"),
            Map.entry("gz", "application/gzip"),
            Map.entry("json", "application/json"),
            Map.entry("xml", "application/xml"),
            Map.entry("txt", "text/plain"),
            Map.entry("csv", "text/csv"),
            Map.entry("html", "text/html"),
            Map.entry("css", "text/css"),
            Map.entry("js", "text/javascript"),
            Map.entry("woff2", "font/woff2"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("mp4", "video/mp4"));

    private MimeTypes() {}

    public static String forFileName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return DEFAULT;
        }
        return BY_EXTENSION.getOrDefault(fileName.substring(dot + 1).toLowerCase(Locale.ROOT), DEFAULT);
    }
}
