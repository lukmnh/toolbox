package io.github.lukmnh.toolbox.devutils.git.domain;

/**
 * Satu opsi git. {@code key} dipakai presentation untuk teks penjelasan; opsi dengan {@code group} sama saling eksklusif
 * (misalnya {@code --soft}/{@code --hard}); {@code dangerous} = bisa menghapus atau menimpa data/riwayat.
 */
public record GitOption(String key, String flag, boolean takesValue, String group, boolean dangerous) {

    public static GitOption flag(String key, String flag) {
        return new GitOption(key, flag, false, "", false);
    }

    public static GitOption value(String key, String flag) {
        return new GitOption(key, flag, true, "", false);
    }

    public GitOption inGroup(String name) {
        return new GitOption(key, flag, takesValue, name, dangerous);
    }

    public GitOption asDangerous() {
        return new GitOption(key, flag, takesValue, group, true);
    }
}
