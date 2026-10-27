package io.github.lukmnh.toolbox.common.download;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DownloadLocationResolverTest {

    @TempDir
    Path home;

    @Test
    void windowsUsesUserProfileDownloads() {
        Path profile = home.resolve("Users").resolve("budi");
        var resolver = new DownloadLocationResolver(true, home, Map.of("USERPROFILE", profile.toString()));

        assertEquals(profile.resolve("Downloads"), resolver.resolve());
    }

    @Test
    void windowsFallsBackToHomeWithoutUserProfile() {
        assertEquals(home.resolve("Downloads"), new DownloadLocationResolver(true, home, Map.of()).resolve());
    }

    @Test
    void linuxReadsXdgDownloadDirAndExpandsHome() throws IOException {
        writeUserDirs(home.resolve(".config"), """
                # komentar
                XDG_DESKTOP_DIR="$HOME/Desktop"
                XDG_DOWNLOAD_DIR="$HOME/Unduhan"
                """);

        assertEquals(home.resolve("Unduhan"), new DownloadLocationResolver(false, home, Map.of()).resolve());
    }

    @Test
    void linuxSupportsBracedHomeAndAbsolutePaths() throws IOException {
        Path configHome = home.resolve("cfg");
        writeUserDirs(configHome, "XDG_DOWNLOAD_DIR=\"${HOME}/Dl\"\n");
        var braced = new DownloadLocationResolver(false, home, Map.of("XDG_CONFIG_HOME", configHome.toString()));
        assertEquals(home.resolve("Dl"), braced.resolve());

        Path absolute = home.resolve("mnt").resolve("data");
        writeUserDirs(configHome, "XDG_DOWNLOAD_DIR=\"" + absolute + "\"\n");
        assertEquals(absolute, braced.resolve());
    }

    @Test
    void linuxFallsBackToDownloadsWithoutUserDirsFile() {
        assertEquals(home.resolve("Downloads"), new DownloadLocationResolver(false, home, Map.of()).resolve());
    }

    @Test
    void linuxFallsBackWhenKeyIsMissingOrEmpty() throws IOException {
        writeUserDirs(home.resolve(".config"), "XDG_DOWNLOAD_DIR=\"\"\nXDG_MUSIC_DIR=\"$HOME/Music\"\n");

        assertEquals(home.resolve("Downloads"), new DownloadLocationResolver(false, home, Map.of()).resolve());
    }

    private static void writeUserDirs(Path configHome, String content) throws IOException {
        Files.createDirectories(configHome);
        Files.writeString(configHome.resolve("user-dirs.dirs"), content);
    }
}
