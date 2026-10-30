package io.github.lukmnh.toolbox.productivity.text.domain;

import java.util.Set;

/**
 * Kata tugas bahasa Indonesia yang bisa diabaikan di daftar kata terbanyak. Daftarnya lebih luas daripada kata tugas
 * Title Case karena tujuannya beda: di sini kata yang tidak membawa makna topik juga dibuang (ini, itu, adalah, …).
 */
public final class IndonesianStopWords {

    private static final Set<String> WORDS = Set.of(
            "ada",
            "adalah",
            "agar",
            "akan",
            "aku",
            "anda",
            "antara",
            "apa",
            "atau",
            "bagi",
            "bahwa",
            "banyak",
            "belum",
            "beberapa",
            "bisa",
            "dalam",
            "dan",
            "dapat",
            "dari",
            "daripada",
            "dengan",
            "di",
            "dia",
            "hanya",
            "harus",
            "hingga",
            "ia",
            "ini",
            "itu",
            "jika",
            "juga",
            "kalau",
            "kami",
            "kamu",
            "karena",
            "ke",
            "kepada",
            "kita",
            "lagi",
            "lebih",
            "maka",
            "masih",
            "mereka",
            "namun",
            "nya",
            "oleh",
            "pada",
            "para",
            "per",
            "pun",
            "saat",
            "saja",
            "sang",
            "sangat",
            "saya",
            "sebagai",
            "sebuah",
            "sedang",
            "sehingga",
            "sejak",
            "sekali",
            "semua",
            "sendiri",
            "seperti",
            "serta",
            "si",
            "setelah",
            "sudah",
            "tapi",
            "telah",
            "tentang",
            "tersebut",
            "tetapi",
            "tidak",
            "untuk",
            "yaitu",
            "yakni",
            "yang");

    private IndonesianStopWords() {}

    /** @param lowerCaseWord kata yang sudah huruf kecil */
    public static boolean contains(String lowerCaseWord) {
        return WORDS.contains(lowerCaseWord);
    }
}
