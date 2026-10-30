# Flowcharts

Diagram alur utama Toolbox dalam format [Mermaid](https://mermaid.js.org/). GitHub dan IntelliJ IDEA (plugin Mermaid) merender diagram ini secara langsung.

> Diagram ini mengikuti spesifikasi. Kalau implementasi berubah, perbarui diagram di PR yang sama.

## Daftar Isi

- [A. Navigasi App](#a-navigasi-app)
- [B. Ganti Tool](#b-ganti-tool)
- [C. Download Hasil](#c-download-hasil)
- [D. Background Task](#d-background-task)
- [E. JSON Viewer (File Besar)](#e-json-viewer-file-besar)
- [F. JSON Beautify](#f-json-beautify)
- [G. Kompres PDF](#g-kompres-pdf)
- [H. Regex Tester (Timeout)](#h-regex-tester-timeout)
- [I. Checksum Verify](#i-checksum-verify)
- [J. Dependency Rule (Clean Architecture)](#j-dependency-rule-clean-architecture)
- [K. Development → Rilis → Install](#k-development--rilis--install)

---

## A. Navigasi App

```mermaid
flowchart TD
    A[Buka aplikasi] --> ARG{Ada argumen file?}
    ARG -- tidak --> H
    ARG -- ya --> R{Ekstensi?}
    R -- .pdf --> TS["Isi & TTD"]
    R -- ".json / .jsonl / .ndjson" --> TJ[JSON Viewer]
    R -- lainnya --> HT[Home + toast: tipe file tidak didukung]
    HT --> H

    H["Home: grid tool per kategori, search, terakhir dipakai"]
    H --> C1["PDF: Isi & TTD, Gabung, Pisah, Kompres"]
    H --> C2["JSON: Viewer, Beautify"]
    H --> C3["Dev Utils: Hash, Checksum, Base64, Regex, Text Diff"]
    C1 & C2 & C3 --> T["Tool aktif: createView (lazy)"]
    TS & TJ --> T
    T --> OF["openFile(path) bila dibuka via file"]

    T --> D[Download hasil]
    T --> SB["Sidebar: Home / tool lain / Settings"]
    SB --> SW[Ganti tool, lihat diagram B]
    T --> CL[Tutup aplikasi]
    CL --> K{canClose?}
    K -- ya --> X[dispose + simpan state window] --> END[Keluar]
    K -- tidak --> Q{Buang perubahan?}
    Q -- ya --> X
    Q -- batal --> T
```

## B. Ganti Tool

Hanya **satu tool aktif** dalam satu waktu. Tool lama dilepas sepenuhnya sebelum tool baru dibuat supaya memori turun lagi.

```mermaid
sequenceDiagram
    actor U as User
    participant S as Sidebar / Home
    participant N as ToolNavigator
    participant Old as Tool aktif
    participant New as Tool tujuan

    U->>S: Pilih tool / Home / Settings
    S->>N: navigateTo(target)
    N->>Old: canClose()
    alt false (ada perubahan belum disimpan)
        N->>U: Dialog "Buang perubahan?"
        U-->>N: Batal
        N-->>S: Tetap di tool aktif
    else true, atau user pilih "Buang"
        N->>Old: dispose()
        Note over Old: cancel task, tutup PDDocument / FileChannel,<br/>kosongkan cache, hapus temp file
        N->>N: buang referensi view lama
        N->>New: createView(ToolContext)
        N->>N: catat ke recent.json
        N-->>U: Tampilkan tool baru + highlight di sidebar
    end
```

## C. Download Hasil

```mermaid
flowchart TD
    P[Proses selesai di background] --> F[Output ditulis ke temp file]
    F --> BTN{Tombol yang diklik}
    BTN -- Download --> A{"Setting: selalu tanya lokasi?"}
    BTN -- "Simpan sebagai…" --> FC[FileChooser]
    A -- tidak --> U["Folder download default + nama unik: hasil.pdf, hasil (1).pdf, …"]
    A -- ya --> FC
    FC -- batal --> Z[Temp tetap ada, bisa download ulang]
    U & FC --> MULTI{Output multi-file?}
    MULTI -- ya --> SUB["Subfolder nama-file_split/"] --> M
    MULTI -- tidak --> M["Atomic move ke tujuan (fallback: move biasa)"]
    M -- gagal --> ERR[Dialog error + nama file] --> Z
    M -- sukses --> N[Toast: Buka file / Buka folder]
```

Resolusi folder download default:

```mermaid
flowchart TD
    S{Settings: folder custom?} -- ya --> C[Pakai folder custom]
    S -- tidak --> OS{OS?}
    OS -- Windows --> W["%USERPROFILE%\Downloads"]
    OS -- Linux --> X{"~/.config/user-dirs.dirs ada XDG_DOWNLOAD_DIR?"}
    X -- ya --> XE["Expand $HOME → path"]
    X -- tidak --> XF["~/Downloads"]
```

## D. Background Task

Semua proses berat (PDF, indexing JSON, checksum, diff, regex) berjalan lewat `TaskRunner`. UI hanya diperbarui di FX thread.

```mermaid
stateDiagram-v2
    [*] --> Idle
    Idle --> Running: submit task, kontrol di-disable, progress tampil
    Running --> Running: progress update (FX thread)
    Running --> Succeeded: selesai
    Running --> Cancelled: tombol Batal / dispose()
    Running --> Failed: exception
    Succeeded --> Idle: tampilkan hasil + aktifkan Download
    Cancelled --> Idle: bersihkan temp, kontrol aktif lagi
    Failed --> Idle: ErrorDialogs (pesan Indonesia + nama file), log ke <folder aplikasi>/logs
```

## E. JSON Viewer (File Besar)

Target: 400 ribu record (~200 MB) dengan total RAM aplikasi < 400 MB. File tidak pernah dimuat utuh ke memori.

```mermaid
flowchart LR
    F[File .json / .jsonl / .ndjson] --> T{Format?}
    T -- array top-level --> IA["Indexer JsonParser streaming: offset awal/akhir tiap elemen"]
    T -- JSONL/NDJSON --> IL["Scan byte \n"]
    T -- root non-array --> TR[Mode Tree: lazy expand]
    IA & IL --> IX[("Index offset: LongArrayList")]
    IX --> V["TableView virtual: render baris terlihat saja"]
    V --> RD["FileChannel.read(position) per record"] --> L[("LRU cache ~2.000 record")]
    IX --> Q["Filter query / Sort kolom (background, bisa cancel)"] --> IDX[("int[] hasil")] --> V
    IX --> QS["Quick search: contains di raw bytes"] --> IDX
    IDX --> E["Download: JSON array / JSONL / CSV (streaming)"]
```

## F. JSON Beautify

```mermaid
flowchart TD
    IN{Sumber input} -- Paste --> TXT[TextArea]
    IN -- "Upload / drag-drop" --> EXT{"Ekstensi .json? (case-insensitive)"}
    EXT -- tidak --> REJ["Tolak + pesan jelas (.jsonl, .txt juga ditolak)"]
    EXT -- ya --> FILE[File input]
    TXT & FILE --> ACT{Aksi}
    ACT -- Validate --> P[JsonParser streaming]
    ACT -- "Beautify / Minify" --> SK{"Sort keys aktif?"}
    SK -- "ya (input ≤ 50 MB)" --> TREE[Parse ke tree, sort, tulis ke temp]
    SK -- tidak --> STR["JsonParser → JsonGenerator.copyCurrentStructure → temp file"]
    P & TREE & STR --> OK{Valid?}
    OK -- tidak --> ER["Error baris:kolom + konteks ±40 char + penanda posisi"]
    OK -- ya --> OUT["Preview 1 MB pertama + info ukuran, jumlah key/elemen, durasi"]
    OUT --> DL["Copy (≤ 10 MB) / Download / Simpan sebagai…"]
```

## G. Kompres PDF

```mermaid
flowchart TD
    S[Pilih PDF + preset] --> LD[Load dari bytes] --> PW{Berpassword?}
    PW -- ya --> DPW[Dialog password] --> LD
    PW -- tidak --> SCAN["PDFStreamEngine: operator Do + CTM → effective DPI (rekursif ke Form XObject)"]
    SCAN --> IMG{Untuk tiap image}
    IMG --> SKIP{"SMask/alpha, image mask, 1-bit (CCITT/JBIG2)?"}
    SKIP -- ya --> KEEP[Biarkan]
    SKIP -- tidak --> DPI{"DPI > target + 20?"}
    DPI -- ya --> DS["Downscale bicubic → JPEG (quality preset)"]
    DPI -- tidak --> FL{"Flate 8-bit RGB/Gray ≥ 64×64?"}
    FL -- ya --> JP[Coba JPEG]
    FL -- tidak --> KEEP
    DS & JP --> SMALL{"Hasil lebih kecil? (JPEG dari Flate: ≥ 20%)"}
    SMALL -- ya --> REP["Ganti image (cache per COSStream)"]
    SMALL -- tidak --> KEEP
    REP & KEEP --> SAVE[Simpan ke temp]
    SAVE --> CMP{Total lebih kecil dari asli?}
    CMP -- ya --> RES["Tampilkan ukuran sebelum → sesudah (%)"]
    CMP -- tidak --> OPT["Hasil = salinan asli + info 'sudah optimal'"]
    RES & OPT --> DL["Download nama-file_compressed.pdf"]
```

Preset:

| Preset | DPI target | Kualitas JPEG |
|---|---|---|
| Seimbang (default) | 150 | 0.75 |
| Kualitas tinggi | 200 | 0.85 |
| Ukuran kecil | 110 | 0.60 |

## H. Regex Tester (Timeout)

```mermaid
flowchart TD
    IN[Pattern / flag / teks berubah] --> DB[Debounce 300 ms]
    DB --> CMP{Pattern valid?}
    CMP -- tidak --> SYN["PatternSyntaxException → deskripsi + penanda ^ di index"]
    CMP -- ya --> RUN["Eksekusi di background, teks dibungkus CharSequence yang cek Thread.interrupted()"]
    RUN --> TO{"Selesai dalam 2 detik?"}
    TO -- tidak --> INT[Interrupt thread] --> MSG["'Regex timeout (kemungkinan backtracking berlebihan)'"]
    TO -- ya --> OUT["Highlight (maks 1.000 match / 200 KB) + tabel match & group + total"]
    OUT --> RP{Mode Replace?}
    RP -- ya --> PRV["Preview hasil replace ($1, ${name})"]
```

## I. Checksum Verify

```mermaid
flowchart TD
    M{Mode verify} -- "(a) satu file" --> A1[Pilih file + paste checksum expected]
    A1 --> AD{"Auto-detect dari panjang: 8=CRC32, 32=MD5, 40=SHA-1, 64=SHA-256, 128=SHA-512"}
    AD --> OV[User boleh override algoritma]
    OV --> H1["Hitung streaming (buffer 1 MB)"]
    H1 --> C1{"Sama? (trim, case-insensitive)"}
    C1 -- ya --> OK[COCOK]
    C1 -- tidak --> NO[TIDAK COCOK]

    M -- "(b) file SUMS" --> B1["Load *SUMS / .sha256"]
    B1 --> B2[Parse baris: hash + nama file]
    B2 --> B3{File ada? relatif ke folder SUMS}
    B3 -- tidak --> NF[TIDAK DITEMUKAN]
    B3 -- ya --> B4[Hitung hash] --> B5{Sama?}
    B5 -- ya --> BOK[OK]
    B5 -- tidak --> BF[GAGAL]
```

## J. Dependency Rule (Clean Architecture)

Ditegakkan oleh `ArchitectureTest` (ArchUnit) di setiap module.

```mermaid
flowchart LR
    P["presentation: View + ViewModel"] --> A["application: use case"]
    P --> D
    A --> D["domain: model + port (JDK saja)"]
    I["infrastructure: PDFBox / Jackson / FS / diff-utils"] -. implements port .-> D
    CR["*Provider: composition root"] --> P & A & I
    P --> CUI[common.ui]
    A --> CT[common.task]
```

Dependency antar module (satu arah):

```mermaid
flowchart LR
    APP[app] --> PDF[tool-pdf] & JSON[tool-json] & DEV[tool-devutils]
    PDF & JSON & DEV --> COMMON[common]
    APP --> COMMON
```

Antar module `tool-*` tidak boleh saling depend. Antar folder tool di dalam satu module (misalnya `merge/` dan `split/`) tidak boleh saling import. Kode yang dipakai bersama masuk ke `shared/`.

## K. Development → Rilis → Install

```mermaid
flowchart LR
    B["Branch feat/... atau fix/..."] --> PR[Pull request] --> CI{"CI: ./mvnw verify Ubuntu + Windows"}
    CI -- gagal --> B
    CI -- hijau --> MG[Squash merge ke main]
    MG --> TG["versions:set + CHANGELOG + tag vX.Y.Z"]
    TG --> RL["Release workflow: .deb, tar.gz, zip"]
    RL --> GR[GitHub Releases]
    GR --> UB["Ubuntu: apt install .deb / extract tar.gz"]
    GR --> WN["Windows: extract zip"]
```
