package io.github.lukmnh.toolbox.common.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Membatasi akses file/folder hanya untuk user pemilik (Security Requirements #1 dan #4). */
public final class OwnerOnlyFiles {

    private static final Set<PosixFilePermission> OWNER_FILE = PosixFilePermissions.fromString("rw-------");
    private static final Set<PosixFilePermission> OWNER_DIRECTORY = PosixFilePermissions.fromString("rwx------");

    private OwnerOnlyFiles() {}

    /** Buat folder (beserta parent) lalu batasi folder terakhir ke owner saja. */
    public static Path createDirectories(Path directory) throws IOException {
        Files.createDirectories(directory);
        restrict(directory);
        return directory;
    }

    /**
     * Linux/macOS: {@code chmod 600} (file) atau {@code 700} (folder). Windows: ACL diganti dengan satu entry
     * owner berakses penuh, sehingga entry bawaan (misalnya grup Users) tidak berlaku lagi.
     *
     * @throws IOException bila file system tidak mendukung POSIX maupun ACL; tidak boleh diam-diam diabaikan
     */
    public static void restrict(Path path) throws IOException {
        boolean directory = Files.isDirectory(path);
        if (Files.getFileAttributeView(path, PosixFileAttributeView.class) != null) {
            Files.setPosixFilePermissions(path, directory ? OWNER_DIRECTORY : OWNER_FILE);
            return;
        }
        AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class);
        if (acl == null) {
            throw new IOException("File system does not support owner-only permissions: " + path.getFileName());
        }
        AclEntry ownerOnly = AclEntry.newBuilder()
                .setType(AclEntryType.ALLOW)
                .setPrincipal(acl.getOwner())
                .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                .setFlags(
                        directory
                                ? EnumSet.of(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT)
                                : EnumSet.noneOf(AclEntryFlag.class))
                .build();
        acl.setAcl(List.of(ownerOnly));
    }
}
