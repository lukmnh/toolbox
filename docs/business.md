# Kebutuhan Bisnis — Toolbox

| | |
|---|---|
| **Produk** | Toolbox: superapp utility pribadi berbasis desktop |
| **Versi dokumen** | 0.1 (untuk rilis `v0.1.0`) |
| **Pemilik** | lukmnh |
| **Status** | Draft, belum ada implementasi |

Dokumen ini menjelaskan **apa** yang harus dilakukan Toolbox dan **kenapa**. Detail teknis (layer, library, packaging) ada di [README](../README.md). Alur visualnya ada di [flowcharts.md](flowcharts.md).

---

## 1. Latar Belakang

Pekerjaan harian sering butuh utilitas kecil: menandatangani dan mengisi PDF, menggabung atau memisah PDF, membaca file JSON besar, menghitung hash, mengecek checksum, menguji regex, membandingkan teks, dan encode/decode Base64.

Kondisi saat ini:

- **Tersebar di banyak tempat.** Situs web, aplikasi terpisah, dan command line berbeda-beda untuk tiap kebutuhan.
- **Risiko privasi.** Situs online mengharuskan dokumen (kontrak, data internal, log) diunggah ke server pihak ketiga.
- **Tidak kuat untuk file besar.** Viewer JSON online dan editor teks umumnya freeze atau crash pada file ratusan MB.
- **Beda OS.** Laptop kantor memakai Ubuntu, komputer rumah memakai Windows 11. Tool favorit di satu OS sering tidak ada di OS lain.

## 2. Tujuan

| ID | Tujuan | Ukuran keberhasilan |
|---|---|---|
| G-1 | Satu aplikasi untuk semua utilitas harian | 11 tool tersedia dari satu menu Home |
| G-2 | Dokumen tidak pernah keluar dari komputer | 100% offline, tanpa network call |
| G-3 | Tetap lancar untuk file besar | JSON 400 ribu record (~200 MB) bisa dibuka, difilter, dan di-download dengan RAM aplikasi < 400 MB, UI tidak freeze |
| G-4 | Sama di Windows dan Ubuntu | Fitur dan hasil identik di Windows 11 dan Ubuntu 22.04/24.04 |
| G-5 | Mudah di-install dan di-update | Installer `.deb`, portable `tar.gz` dan `.zip` dari GitHub Releases |
| G-6 | Mudah ditambah tool baru | Tool baru cukup menambah folder dan implementasi `Tool`, tanpa mengubah shell |

## 3. Ruang Lingkup

### 3.1 Termasuk (v0.1.0)

| Kategori | Tool |
|---|---|
| PDF | Isi & TTD, Gabung, Pisah, Kompres |
| JSON | JSON Viewer, JSON Beautify |
| Dev Utils | Hash Generator, Checksum, Regex Tester, Text Diff, Base64 |

Ditambah shell aplikasi: Home, sidebar, Settings, Open with, dan Download terpusat.

### 3.2 Tidak Termasuk

- Database, akun, login, atau sinkronisasi cloud.
- Tanda tangan digital kriptografis (sertifikat/PKI). "TTD" di Toolbox adalah **gambar tanda tangan** yang ditempel ke halaman.
- OCR, edit teks asli di dalam PDF, dan konversi PDF ke format lain.
- Edit isi JSON di JSON Viewer (read-only).
- Auto-update di dalam aplikasi. Update dilakukan manual dari GitHub Releases.
- macOS (tidak diuji dan tidak ada paket rilis).

## 4. Pengguna dan Konteks

| Aspek | Keterangan |
|---|---|
| Pengguna | Satu orang (pemilik), developer, nyaman dengan teknis |
| Perangkat | Laptop kantor Ubuntu 22.04/24.04, komputer rumah Windows 11 |
| Layar | Minimal 1200×800 |
| Bahasa UI | Bahasa Indonesia |
| Koneksi | Tidak dibutuhkan |

### Skenario Utama

| ID | Skenario |
|---|---|
| S-1 | Menerima PDF formulir dari HR, mengisi field, menempel tanda tangan, lalu mengirim balik |
| S-2 | Menggabungkan beberapa scan dokumen menjadi satu PDF dan mengecilkan ukurannya sebelum diunggah ke portal yang membatasi ukuran file |
| S-3 | Membuka dump JSON/JSONL ratusan MB dari sistem, memfilter record tertentu, lalu mengekspor hasilnya ke CSV |
| S-4 | Merapikan atau memvalidasi JSON dari API sebelum dibagikan |
| S-5 | Memverifikasi file ISO/installer yang diunduh dengan file `SHA256SUMS` |
| S-6 | Menguji regex untuk parsing log sebelum dipakai di kode Java |
| S-7 | Membandingkan dua versi config dan menghasilkan file `.diff` yang bisa di-`git apply` |
| S-8 | Mengubah gambar ke data URI Base64, atau sebaliknya |

---

## 5. Kebutuhan Fungsional

### 5.1 Shell Aplikasi

| ID | Kebutuhan |
|---|---|
| FR-SH-1 | Home menampilkan kartu tool yang dikelompokkan per kategori, kotak search, dan section "Terakhir dipakai" (maks 4) |
| FR-SH-2 | Saat tool dibuka, tampil konten tool penuh dengan sidebar yang bisa di-collapse (Home, daftar tool per kategori dengan tool aktif di-highlight, Settings) |
| FR-SH-3 | Hanya satu tool aktif dalam satu waktu. Saat pindah tool, tool lama dilepas dari memori |
| FR-SH-4 | Kalau tool punya perubahan yang belum di-download, sistem minta konfirmasi "Buang perubahan?" sebelum pindah tool atau menutup aplikasi |
| FR-SH-5 | Shortcut `Ctrl+H` ke Home dan `Ctrl+B` untuk toggle sidebar |
| FR-SH-6 | **Open with**: file `.pdf` membuka Isi & TTD, file `.json`/`.jsonl`/`.ndjson` membuka JSON Viewer, ekstensi lain membuka Home dengan pesan "tipe file tidak didukung" |
| FR-SH-7 | Ukuran dan posisi window serta state sidebar diingat antar sesi |
| FR-SH-8 | Settings: folder download default, toggle "Selalu tanya lokasi", dan tema terang/gelap (default mengikuti OS) |

### 5.2 Download

Berlaku untuk semua tool PDF, JSON, Base64, dan Text Diff.

| ID | Kebutuhan |
|---|---|
| FR-DL-1 | Tombol **Download** menyimpan hasil ke folder download default tanpa dialog |
| FR-DL-2 | Tombol **Simpan sebagai…** membuka dialog pilih lokasi |
| FR-DL-3 | Setelah berhasil, muncul notifikasi dengan tombol "Buka file" dan "Buka folder" |
| FR-DL-4 | Kalau user membatalkan dialog, hasil tidak hilang dan bisa di-download ulang |

### 5.3 Tool PDF

| ID | Tool | Kebutuhan |
|---|---|---|
| FR-PDF-1 | Isi & TTD | Preview per halaman dengan navigasi dan zoom |
| FR-PDF-2 | Isi & TTD | Tambah teks bebas di posisi klik: multi-line, ukuran font bisa diatur, bisa diedit, digeser, dan dihapus |
| FR-PDF-3 | Isi & TTD | Isi form PDF (text field, checkbox, radio, combo/list) dari panel samping |
| FR-PDF-4 | Isi & TTD | Buat tanda tangan dengan menggambar (warna hitam/biru, tebal pena) atau upload gambar PNG/JPG |
| FR-PDF-5 | Isi & TTD | Tempel TTD di posisi klik, bisa digeser dan di-resize dengan aspek tetap |
| FR-PDF-6 | Isi & TTD | Tanda tangan terakhir diingat dan dimuat otomatis |
| FR-PDF-7 | Gabung | Gabung banyak PDF dengan urutan yang bisa diatur (drag-drop, naik/turun) |
| FR-PDF-8 | Pisah | Pisah per halaman, atau per range seperti `1-3, 5, 8-10` (tiap range jadi satu file) |
| FR-PDF-9 | Kompres | Kompres dengan tiga preset kualitas, tampilkan ukuran sebelum → sesudah |
| FR-PDF-10 | Semua | PDF berpassword: minta password, lalu proses. Hasil disimpan tanpa password |

### 5.4 Tool JSON

| ID | Tool | Kebutuhan |
|---|---|---|
| FR-JS-1 | Viewer | Buka JSON array, JSONL/NDJSON, dan JSON non-array (mode tree) |
| FR-JS-2 | Viewer | Mode tabel dengan kolom otomatis dari key, dan panel detail record (pretty dan tree) |
| FR-JS-3 | Viewer | Quick search teks, filter query (`field op value` + `AND`/`OR`/kurung), dan sort per kolom |
| FR-JS-4 | Viewer | Download hasil view (setelah filter/sort) sebagai JSON array, JSONL, atau CSV |
| FR-JS-5 | Viewer | Copy cell, copy row sebagai JSON, lompat ke nomor record, dan daftar file terakhir (maks 10) |
| FR-JS-6 | Viewer | Status bar: ukuran file, jumlah total/terfilter, waktu indexing, pemakaian memori |
| FR-JS-7 | Beautify | Input dari paste atau upload file `.json` |
| FR-JS-8 | Beautify | Aksi Beautify, Minify, Validate. Opsi indent 2/4/tab dan sort keys |
| FR-JS-9 | Beautify | Error validasi menunjuk baris:kolom dengan potongan konteks |
| FR-JS-10 | Beautify | Copy, Download, dan Simpan sebagai… |

### 5.5 Tool Dev Utils

| ID | Tool | Kebutuhan |
|---|---|---|
| FR-DU-1 | Hash | Hash teks dengan MD5, SHA-1, SHA-256, SHA-384, SHA-512, SHA3-256, SHA3-512 sekaligus, update live |
| FR-DU-2 | Hash | Format output hex lowercase, HEX UPPERCASE, Base64. Copy per baris |
| FR-DU-3 | Hash | Mode HMAC dengan secret key |
| FR-DU-4 | Checksum | Generate checksum banyak file (SHA-256, SHA-1, SHA-512, MD5, CRC32) dan download sebagai file SUMS |
| FR-DU-5 | Checksum | Verify satu file terhadap checksum yang di-paste, dengan hasil COCOK / TIDAK COCOK |
| FR-DU-6 | Checksum | Verify semua file yang tercantum di file `*SUMS` / `.sha256` |
| FR-DU-7 | Regex | Uji regex Java dengan flag, highlight match, tabel match dan group (termasuk named group) |
| FR-DU-8 | Regex | Mode Replace dengan preview |
| FR-DU-9 | Regex | Cheat sheet regex, dan copy pattern sebagai Java string literal |
| FR-DU-10 | Text Diff | Bandingkan dua teks/file: tampilan side-by-side dan unified, highlight per kata |
| FR-DU-11 | Text Diff | Opsi abaikan whitespace, huruf besar-kecil, dan perbedaan line ending |
| FR-DU-12 | Text Diff | Navigasi perubahan (`F7` / `Shift+F7`), statistik tambah/hapus/ubah |
| FR-DU-13 | Text Diff | Copy dan download unified diff (`.diff`) |
| FR-DU-14 | Base64 | Encode/decode teks: charset, varian Standard/URL-safe/MIME, opsi tanpa padding |
| FR-DU-15 | Base64 | Encode file ke Base64 (opsi data URI). Decode Base64 ke file dengan deteksi tipe dan preview gambar |

---

## 6. Aturan Bisnis

Aturan yang menentukan perilaku sistem. Setiap aturan harus tercakup oleh unit test atau checklist tes manual.

### 6.1 Umum

| ID | Aturan |
|---|---|
| BR-1 | **File asli tidak pernah diubah.** Semua hasil ditulis sebagai file baru |
| BR-2 | File asli tidak boleh ter-lock selama dibuka (penting di Windows) |
| BR-3 | Hasil ditulis ke file sementara dulu, lalu dipindah ke tujuan. File tujuan tidak pernah setengah jadi |
| BR-4 | Nama hasil tidak menimpa file yang sudah ada: `hasil.pdf`, `hasil (1).pdf`, `hasil (2).pdf`, … |
| BR-5 | Folder download default: Windows `%USERPROFILE%\Downloads`. Linux mengikuti `XDG_DOWNLOAD_DIR`, dengan fallback `~/Downloads`. Bisa diganti di Settings |
| BR-6 | Semua pesan error dalam bahasa Indonesia dan menyebut nama file terkait. Tidak ada kegagalan yang diam-diam |
| BR-7 | Proses berat bisa dibatalkan. Selama berjalan, kontrol yang relevan dinonaktifkan |

### 6.2 Penamaan Hasil

| Tool | Nama file hasil |
|---|---|
| Isi & TTD | `<nama>_edited.pdf` |
| Gabung | `merged.pdf` |
| Pisah | Folder `<nama>_split/` berisi `<nama>_hal1-3.pdf`, … |
| Kompres | `<nama>_compressed.pdf` |
| JSON Viewer | Sesuai format: `.json`, `.jsonl`, `.csv` |
| JSON Beautify | `<nama>.pretty.json` / `<nama>.min.json` |
| Checksum | `SHA256SUMS` (mengikuti algoritma) |
| Text Diff | `<nama>.diff` |
| Base64 encode | `<nama>.b64.txt` |
| Base64 decode | `decoded.<ext>` (ext dari magic bytes, default `.bin`) |

### 6.3 PDF

| ID | Aturan |
|---|---|
| BR-PDF-1 | Teks dan TTD di hasil PDF harus berada **persis** di posisi preview, termasuk pada halaman yang di-rotate (0/90/180/270) |
| BR-PDF-2 | Font preview dan font di PDF harus sama, supaya ukuran dan posisi teks konsisten |
| BR-PDF-3 | Overlay (teks/TTD) hanya ditempel ke **salinan** dokumen saat download. Dokumen yang sedang diedit tetap bisa diubah |
| BR-PDF-4 | Gambar TTD yang di-upload tanpa transparansi otomatis dihapus background putihnya. Warna tinta asli dipertahankan |
| BR-PDF-5 | Range halaman tidak valid (di luar jumlah halaman, terbalik, format salah) ditolak dengan pesan yang jelas |
| BR-PDF-6 | Kompres tidak boleh menurunkan kualitas teks/vektor dan tidak menyentuh gambar bertransparansi atau gambar 1-bit (hasil scan hitam-putih) |
| BR-PDF-7 | Kalau hasil kompres tidak lebih kecil, hasil = salinan file asli + info "sudah optimal" |

Preset kompres:

| Preset | Resolusi gambar | Kualitas JPEG | Kapan dipakai |
|---|---|---|---|
| Seimbang (default) | 150 dpi | 0.75 | Umum, tetap enak dibaca dan dicetak |
| Kualitas tinggi | 200 dpi | 0.85 | Dokumen dengan foto/detail halus |
| Ukuran kecil | 110 dpi | 0.60 | Portal dengan batas ukuran ketat |

### 6.4 JSON

| ID | Aturan |
|---|---|
| BR-JS-1 | JSON Beautify **hanya menerima file `.json`** (case-insensitive). File `.jsonl`, `.txt`, dan lainnya ditolak dengan pesan jelas |
| BR-JS-2 | Sort keys hanya tersedia untuk input ≤ 50 MB. Di atas itu opsi dinonaktifkan dengan penjelasan |
| BR-JS-3 | Preview output dibatasi 1 MB pertama, dengan notice "preview terpotong, download untuk file lengkap" |
| BR-JS-4 | Tombol Copy hanya aktif kalau output ≤ 10 MB |
| BR-JS-5 | CSV mengikuti RFC 4180. Nilai nested ditulis sebagai string JSON |
| BR-JS-6 | Mode tabel mengambil kolom dari key top-level 1.000 record pertama. Preview nilai nested maks 200 karakter |

### 6.5 Dev Utils

| ID | Aturan |
|---|---|
| BR-DU-1 | MD5 dan SHA-1 diberi label "tidak aman untuk keamanan" |
| BR-DU-2 | Secret key HMAC **tidak pernah** disimpan ke disk atau ditulis ke log |
| BR-DU-3 | Auto-detect algoritma checksum dari panjang hex: 8 = CRC32, 32 = MD5, 40 = SHA-1, 64 = SHA-256, 128 = SHA-512. Perbandingan case-insensitive dan mengabaikan spasi |
| BR-DU-4 | File SUMS mengikuti format `sha256sum` (`<hash>  <nama-file>`) dan kompatibel dengan `sha256sum -c`. Path di dalamnya relatif ke folder file SUMS |
| BR-DU-5 | Regex yang berjalan lebih dari 2 detik dihentikan dengan pesan "Regex timeout (kemungkinan backtracking berlebihan)". Aplikasi tidak boleh hang |
| BR-DU-6 | Regex: file uji maks 5 MB. Highlight maks 1.000 match / 200 KB pertama |
| BR-DU-7 | Text Diff: maks 20 MB per sisi. File biner (ada byte `0x00` di 8 KB pertama) ditolak |
| BR-DU-8 | Output `.diff` harus valid untuk `git apply` / `patch`, dengan 3 baris konteks |
| BR-DU-9 | Base64 decode toleran terhadap whitespace/newline dan prefix data URI. Karakter tidak valid dilaporkan beserta posisinya |
| BR-DU-10 | Base64 decode ke file mendeteksi tipe dari magic bytes: PNG, JPEG, GIF, WebP, PDF, ZIP, GZIP. Selain itu `.bin` |

---

## 7. Kebutuhan Non-Fungsional

| ID | Kategori | Kebutuhan |
|---|---|---|
| NFR-1 | Privasi | 100% offline. Tidak ada telemetry atau network call |
| NFR-2 | Performa | JSON Viewer 400 ribu record (~200 MB): total RAM aplikasi < 400 MB |
| NFR-3 | Responsif | UI tidak pernah freeze. Semua proses berat di background dengan progress |
| NFR-4 | Memori | Setelah pindah tool, pemakaian memori turun kembali |
| NFR-5 | Skala | Checksum aman untuk file berukuran GB. Text Diff lancar untuk 100 ribu baris. Beautify aman untuk file ratusan MB |
| NFR-6 | Platform | Windows 11 dan Ubuntu 22.04/24.04 dengan perilaku identik |
| NFR-7 | Bahasa | Semua teks UI berbahasa Indonesia dan terpusat di satu file resource |
| NFR-8 | Tampilan | Modern dan bersih, minimal 1200×800, tema terang dan gelap |
| NFR-9 | Keandalan | Error tercatat di `~/.toolbox/logs/` (rotasi maks 5 file × 1 MB) |
| NFR-10 | Pemeliharaan | Clean Architecture per tool, dijaga oleh test arsitektur otomatis. Tool baru tidak mengubah tool lain |
| NFR-11 | Kualitas | Setiap perubahan lulus CI (build, test, format) di Ubuntu dan Windows sebelum merge |

## 8. Data dan Persistensi

Tanpa database. Semua data lokal di folder home user:

| Lokasi | Isi | Keterangan |
|---|---|---|
| `~/.toolbox/settings.json` | Folder download, "selalu tanya lokasi", tema, ukuran/posisi window, state sidebar | |
| `~/.toolbox/recent.json` | Tool terakhir dipakai, file JSON terakhir dibuka | Maks 4 tool, 10 file |
| `~/.toolbox/signature.png` | Tanda tangan terakhir | Bisa ditimpa kapan saja |
| `~/.toolbox/logs/` | Log aplikasi | Tidak berisi isi dokumen atau secret key |

File sementara dibersihkan saat tool ditutup atau aplikasi keluar.

## 9. Kriteria Penerimaan Rilis v0.1.0

Rilis dianggap siap kalau semua poin di bawah lulus di **Ubuntu dan Windows**:

- [ ] `./mvnw verify` hijau di CI (Ubuntu + Windows).
- [ ] Semua 11 tool muncul di Home dan bisa dibuka dari sidebar.
- [ ] Open with `.pdf` dan `.json` membuka tool yang benar. Ekstensi lain menampilkan pesan.
- [ ] PDF ter-rotate: teks dan TTD di hasil tepat di posisi preview.
- [ ] PDF dengan form: field terisi di hasil download.
- [ ] PDF hasil scan bisa dikompres tanpa merusak teks.
- [ ] JSON 400 ribu record: buka, filter, sort, dan download CSV dengan RAM < 400 MB (cek di status bar dan System Monitor).
- [ ] Upload file non-`.json` di Beautify ditolak.
- [ ] Regex `(a+)+$` pada input panjang berhenti dengan pesan timeout, aplikasi tetap responsif.
- [ ] Text Diff dua file besar lancar, dan `.diff` hasilnya bisa di-`git apply`.
- [ ] Base64 decode gambar: preview muncul dan ekstensi benar.
- [ ] Ganti-ganti tool berkali-kali: RAM turun kembali.
- [ ] Paket `.deb`, `tar.gz`, dan `.zip` ter-upload di GitHub Releases dan bisa di-install.

## 10. Rencana Rilis

| Fase | Isi | Hasil |
|---|---|---|
| 0 | Persiapan | Tool terinstall, repo GitHub siap |
| 1 | Fondasi: struktur Maven, `common`, shell `app`, CI | App jalan, Home tampil, Settings dan download berfungsi |
| 2 | Hash, Checksum, Base64 | 3 tool berfungsi dan bisa download |
| 3 | Regex, Text Diff | Regex timeout aman, `.diff` bisa di-`git apply` |
| 4 | JSON Beautify, JSON Viewer | File 400 ribu record lancar |
| 5 | Gabung, Pisah, Kompres PDF | 3 tool PDF berfungsi |
| 6 | Isi & TTD | Isi form + TTD presisi, termasuk halaman rotate |
| 7 | Packaging, release workflow, dokumentasi | Rilis `v0.1.0` |

Versi mengikuti [SemVer](https://semver.org/): MAJOR untuk perubahan yang merusak kompatibilitas data/setting, MINOR untuk tool atau fitur baru, PATCH untuk perbaikan bug.

## 11. Asumsi dan Risiko

| Jenis | Keterangan | Mitigasi |
|---|---|---|
| Asumsi | Pengguna memakai Liberica JDK 21 Full untuk development. User akhir memakai paket rilis yang sudah membawa runtime | Dijelaskan di README |
| Asumsi | Font Arial (Windows) atau Liberation Sans/DejaVu Sans (Ubuntu) tersedia | Fallback ke Helvetica bawaan PDF |
| Risiko | PDF tertentu punya form atau appearance yang rusak | Error ditampilkan jelas, aplikasi tidak crash |
| Risiko | Kompres tidak efektif untuk PDF yang isinya teks/vektor | Tampilkan info "sudah optimal" |
| Risiko | RAM melebihi target pada JSON dengan record sangat besar | Preview detail dibatasi 1 MB, cache record dibatasi |
| Risiko | Perilaku file system berbeda antara Windows dan Linux (lock, atomic move) | Load PDF dari memory, atomic move dengan fallback, CI di dua OS |

## 12. Glosarium

| Istilah | Arti |
|---|---|
| TTD | Tanda tangan, dalam bentuk gambar |
| AcroForm | Form interaktif standar di dalam PDF |
| JSONL / NDJSON | Format satu objek JSON per baris |
| SUMS | File daftar checksum, misalnya `SHA256SUMS` |
| HMAC | Hash dengan secret key untuk verifikasi integritas dan keaslian |
| Data URI | String `data:<mime>;base64,...` yang memuat isi file langsung |
| Magic bytes | Byte awal file yang menandakan tipenya |
| Unified diff | Format diff standar (`---`/`+++`/`@@`) yang dipakai `git` dan `patch` |
| Open with | Membuka file langsung ke tool yang sesuai dari file manager |
