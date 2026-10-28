package io.github.lukmnh.toolbox.devutils.checksum.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Format file daftar checksum GNU coreutils: {@code <hash>  <nama-file>} (mode teks) atau {@code <hash> *<nama-file>}
 * (mode biner). Hasil {@link #write} bisa langsung dicek dengan {@code sha256sum -c}.
 */
public final class SumsFormat {

    private static final char BYTE_ORDER_MARK = '﻿';
    private static final String COMMENT_PREFIX = "#";
    private static final char TEXT_MODE = ' ';
    private static final char BINARY_MODE = '*';
    private static final String SEPARATOR = "  ";

    private SumsFormat() {}

    /** @throws InvalidSumsFileException bila ada baris yang salah format atau tidak ada entry sama sekali */
    public static List<SumsEntry> parse(String text) {
        String content = !text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK ? text.substring(1) : text;
        String[] lines = content.split("\n", -1);
        List<SumsEntry> entries = new ArrayList<>();
        for (int index = 0; index < lines.length; index++) {
            String line = stripCarriageReturn(lines[index]);
            if (line.isBlank() || line.startsWith(COMMENT_PREFIX)) {
                continue;
            }
            entries.add(parseLine(line, index + 1));
        }
        if (entries.isEmpty()) {
            throw new InvalidSumsFileException(InvalidSumsFileException.NO_ENTRIES);
        }
        return List.copyOf(entries);
    }

    public static String write(List<SumsEntry> entries) {
        StringBuilder text = new StringBuilder();
        for (SumsEntry entry : entries) {
            text.append(entry.checksum())
                    .append(SEPARATOR)
                    .append(entry.fileName())
                    .append('\n');
        }
        return text.toString();
    }

    private static SumsEntry parseLine(String line, int lineNumber) {
        int space = line.indexOf(' ');
        // Minimal: hash, spasi, penanda mode, lalu nama file 1 karakter
        if (space <= 0 || line.length() < space + 3) {
            throw new InvalidSumsFileException(lineNumber);
        }
        String checksum = line.substring(0, space);
        char mode = line.charAt(space + 1);
        if (mode != TEXT_MODE && mode != BINARY_MODE) {
            throw new InvalidSumsFileException(lineNumber);
        }
        if (ChecksumAlgorithm.detect(checksum).isEmpty()) {
            throw new InvalidSumsFileException(lineNumber);
        }
        return new SumsEntry(checksum, line.substring(space + 2));
    }

    private static String stripCarriageReturn(String line) {
        return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
    }
}
