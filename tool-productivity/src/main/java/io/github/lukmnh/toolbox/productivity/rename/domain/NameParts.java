package io.github.lukmnh.toolbox.productivity.rename.domain;

/** Nama file dipecah jadi stem dan ekstensi terakhir; {@code .bashrc} dan {@code arsip.} tidak punya ekstensi. */
record NameParts(String stem, String extension) {

    static NameParts of(String name) {
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return new NameParts(name, "");
        }
        return new NameParts(name.substring(0, dot), name.substring(dot + 1));
    }

    String fullName() {
        return extension.isEmpty() ? stem : stem + '.' + extension;
    }

    NameParts withStem(String newStem) {
        return new NameParts(newStem, extension);
    }

    NameParts withExtension(String newExtension) {
        return new NameParts(stem, newExtension);
    }
}
