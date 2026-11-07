# Arus: Rencana Design System (v2)

Menggantikan v1. Perubahan utama: **aplikasi sudah dibangun** (layout berantakan), jadi rencana ini berupa audit lalu refactor, bukan membangun dari nol. Wireframe 74 layar menjadi **target struktur**.

Dokumen pendamping: `arus-user-flow.md` (kode layar `S-xx`), `arus-erd.md`, canvas wireframe, `arus-claude-code-prompt.md`.

---

## 1. Keputusan Terkunci

| Topik | Keputusan |
|---|---|
| Aksen | `#0009FF`, dipakai hemat (CTA, nav aktif, item terpilih, fokus) |
| Netral | **Tailwind zinc** |
| Font UI | **Plus Jakarta Sans** |
| Font serif | **Lora**, hanya untuk momen editorial/emosional |
| Font mono | **Setara Consolas** (lihat catatan lisensi di bagian 9) |
| Ikon | **Font Awesome** untuk semua ikon, termasuk ikon pilihan pengguna |
| Bentuk | Pill untuk elemen interaktif, rounded rectangle untuk kartu, input, dialog, sheet |
| Karakter | Hitam-putih, playful, haptic interaktif |
| Ilustrasi | Minimal, tebal, gaya sketsa spidol (marker) |
| Tema | Light dulu; token semantik disiapkan agar dark mode bisa menyusul |
| Wireframe | Hanya acuan **struktur dan alur**, bukan acuan visual (gaya tldraw tidak dipakai di UI akhir) |

---

## 2. Token

### 2.1 Warna

**Brand**

| Token | Nilai | Pemakaian |
|---|---|---|
| `primary` | `#0009FF` | CTA utama, nav aktif, item terpilih, fokus |
| `onPrimary` | `#FFFFFF` | Teks pada primary (≈ 8,5:1) |
| `primaryPressed` | gelapkan ±12% | Status ditekan |
| `primarySubtle` | tint sangat muda | Hanya badge kecil dan cincin fokus, **bukan** latar kartu |

**Zinc (Tailwind)**

| Skala | Hex | Token Arus |
|---|---|---|
| 50 | `#FAFAFA` | `surfaceSubtle` |
| 100 | `#F4F4F5` | `surfaceSecondary` |
| 200 | `#E4E4E7` | `surfaceTertiary`, `border` |
| 300 | `#D4D4D8` | `borderStrong` |
| 400 | `#A1A1AA` | `textMuted` (placeholder dan keterangan non-esensial saja) |
| 500 | `#71717A` | `textSecondary` (≈ 4,8:1) |
| 600 | `#52525B` | ikon sekunder |
| 700 | `#3F3F46` | (cadangan) |
| 800 | `#27272A` | permukaan gelap sekunder |
| 900 | `#18181B` | `inverseSurface` |
| 950 | `#09090B` | `textPrimary` |

Nilai heksadesimal di atas adalah skala zinc Tailwind versi 3. Verifikasi terhadap halaman warna Tailwind sebelum dikunci.

`background` dan `surface` = `#FFFFFF`.

**Semantik**

| Token | Nilai | Catatan |
|---|---|---|
| `income` / `incomeText` | `#16A34A` / `#15803D` | Ikon dan bar / teks (varian teks ≈ 5:1) |
| `expense` | `#DC2626` | Selisih, saldo minus, budget terlampaui |
| `warning` / `warningText` | `#D97706` / `#B45309` | Budget 80%, jatuh tempo dekat |
| `info` | `#0009FF` | Sama dengan primary |
| `errorSurface` | tint merah sangat muda | Latar peringatan |

Aturan nominal di daftar: pengeluaran = `textPrimary` dengan tanda minus; pemasukan = `incomeText`; transfer, pelunasan, dan pinjaman = `textSecondary`. Merah dicadangkan untuk selisih, saldo minus, dan budget terlampaui.

### 2.2 Tipografi

| Peran | Font | Ukuran / berat | Dipakai di |
|---|---|---|---|
| Display | Plus Jakarta Sans | 32 / Bold | Net worth, nominal input |
| Heading Large | Plus Jakarta Sans | 24 / Bold | Judul layar besar |
| Heading Medium | Plus Jakarta Sans | 20 / SemiBold | Judul top bar, seksi |
| Heading Small | Plus Jakarta Sans | 18 / SemiBold | Judul kartu |
| Body Large | Plus Jakarta Sans | 16 / Normal | Isi utama |
| Body Medium | Plus Jakarta Sans | 14 / Normal | Isi sekunder |
| Body Small | Plus Jakarta Sans | 12 / Normal | Keterangan |
| Label | Plus Jakarta Sans | 11 / SemiBold | Tag, label seksi |
| Editorial | **Lora** | 24 / SemiBold | Judul onboarding dan kondisi kosong |
| Editorial Quote | **Lora** | 16 / Italic | Kalimat insight, tagline |
| Amount Mono | **Mono** | 16 / Medium | Nominal di daftar dan tabel |
| Code | **Mono** | 14 / Normal | Nomor rekening/telepon di tagihan, kode, pratinjau export |

Aturan penggunaan:
- **Lora** hanya untuk teks pendek yang membawa emosi (onboarding, empty state, sukses). Tidak untuk label, tombol, atau angka.
- **Mono** dipakai bila **rata digit** penting: daftar transaksi, rincian per orang di group order, keypad PIN, Hitung Pecahan. Karena monospace, ini juga menyelesaikan kebutuhan angka tabular tanpa bergantung fitur font.
- Angka hero (Net worth, nominal input) tetap Plus Jakarta Sans agar terasa ramah.
- Tidak ada font lain.

### 2.3 Bentuk, spasi, elevasi

| Token | Nilai |
|---|---|
| `pill` | Bulat penuh (tombol, chip, filter, segmented, badge, indikator nav) |
| `input` | 18dp |
| `card` | 24dp |
| `sheet` / `dialog` | 28dp |
| Spasi | 4, 8, 12, 16, 20, 24, 32, 40dp |
| Padding layar | 16dp horizontal; jarak antarseksi 24dp; antarkartu 12dp; padding kartu 16dp |
| Elevasi | Tanpa bayangan; pemisahan lewat garis `border`, jarak, `surfaceSecondary`. Pengecualian: bottom nav melayang |

---

## 3. Sistem Playful

### 3.1 Ilustrasi sketsa spidol

Gaya: garis hitam (`zinc-950`) tebal 3 sampai 4dp, ujung bulat, sedikit goyang, **satu** isian biru `#0009FF` yang sengaja bergeser dari garis. Latar putih atau zinc-100. Tanpa gradien.

| Dipakai di | Gambar |
|---|---|
| S-02 Sambutan | Tangan memegang dompet / aliran uang |
| S-03, S-04, S-03a onboarding | Satu motif per langkah (opsional) |
| Kondisi kosong | Riwayat, Dompet, Anggaran, Utang-piutang, Group order |
| Sukses | Saldo cocok (S-103), group order lunas |
| Error | OCR gagal, backup gagal |
| S-01 Kunci | Gembok sederhana |

Perkiraan: 10 sampai 12 ilustrasi. Disimpan sebagai vektor (VectorDrawable atau ImageVector), satu berkas per ilustrasi, dinamai konsisten. Ilustrasi bersifat dekoratif (deskripsi konten kosong) kecuali membawa informasi.

### 3.2 Haptic

Semua lewat satu pembungkus `ArusHaptics`; layar tidak boleh memanggil API haptic langsung.

| Peristiwa | Haptic |
|---|---|
| Ketuk tombol utama | Klik ringan |
| Pilih chip, segmen, geser carousel, ketuk keypad PIN | Tick pendek |
| Saklar hidup / mati | Dua pola berbeda |
| Simpan berhasil, saldo cocok, pelunasan tercatat | Konfirmasi |
| Validasi gagal, PIN salah, kredit melewati limit | Penolakan |
| Hapus (sebelum dialog), tahan lama | Peringatan / long press |

Syarat: menghormati pengaturan haptic sistem, ada saklar di Pengaturan, dan memakai jalur kompatibilitas bila API level lama tidak punya tipe tertentu. Ketersediaan tipe tergantung versi Compose dan API level proyek; dicek saat audit.

### 3.3 Gerak

- Tekan: skala turun ±3% dengan pegas.
- Tombol (+): memantul saat muncul dan saat sheet terbuka.
- Berhasil: elemen konfirmasi kecil (centang menggambar diri).
- Semua dimatikan saat "kurangi animasi" aktif (skala durasi animator = 0).
- Tidak ada animasi yang menunda tugas (maksimal 300ms untuk transisi fungsional).

---

## 4. Pemetaan Wireframe ke Komponen

| Pola di wireframe | Komponen | Aturan |
|---|---|---|
| Top bar kembali + judul | `ArusTopBar` | Tanpa aksi kanan |
| Bottom nav melayang + (+) tengah | `ArusBottomNavigation` | Hanya di 4 layar akar tab |
| Tombol hitam penuh di bawah | `ArusPrimaryButton` | Satu aksi utama per layar |
| Tombol sekunder | `ArusSecondaryButton` | Outline, pill |
| Daftar berkartu | `ArusCard` | Maks. satu tingkat kartu |
| Tab segmen | `ArusSegmentedControl` | Pill, terpilih biru solid |
| Chip pilihan | `ArusChip` / `ArusFilterChip` | Tunggal atau ganda |
| Pemilih periode | `FinancialPeriodSelector` | Anggaran, Detail Kategori |
| Nominal besar | `ArusAmountInput` | Format rupiah real-time, keyboard sistem |
| Field teks, cari | `ArusTextField` / `ArusSearchField` | 18dp |
| Saklar | `ArusSwitch` | Biru saat aktif |
| Bar progres | `BudgetProgress` | Normal, 80%+, terlampaui |
| Tag status | `ArusBadge` | Pill, Label 11sp |
| Bottom sheet | `ArusBottomSheet` | 28dp |
| Modal | `ArusDialog` | 28dp |
| Stepper group order | `ArusStepper` | Langkah aktif biru |
| Keypad PIN | `ArusPinKeypad` | S-01, S-05 |
| Baris centang | `ArusCheckRow` | Sentuh ≥ 48dp |
| Carousel dompet | `WalletCarousel` | Kartu berikutnya sedikit terlihat |
| Kartu sorotan | `ArusBalanceCard` | Permukaan gelap (`inverseSurface`), bukan biru |
| Kosong / gagal / memuat | `ArusEmptyState` / `ArusErrorState` / `ArusLoading` | Memakai ilustrasi dan Lora |
| Snackbar Urungkan | `ArusSnackbar` | Setelah simpan dan hapus |

---

## 5. Komponen Domain Keuangan (dari ERD)

Komponen hanya menerima **model UI sederhana**, bukan entity Room.

| Komponen | Sumber ERD | Ketentuan |
|---|---|---|
| `AmountText` | Semua `amount` (Long) | Satu-satunya pemformat rupiah: `Rp 1.250.000`, ringkas `1,2 jt`, negatif `-Rp 65.000`. Memakai font Mono di daftar |
| `TransactionItem` | `transaction.type` | 12 tipe lewat satu pemetaan tipe ke ikon, label, gaya nominal |
| Penanda status | `verified_checkpoint_id`, `occurred_at` | Centang "terverifikasi", label "Terjadwal" |
| `WalletCard` | `wallet.type` | 4 varian; Kredit menampilkan Terpakai, Sisa limit, bar limit |
| `CategoryItem` | `category.kind`, `system_code` | Kategori SYSTEM diberi penanda kunci, tanpa aksi hapus |
| `IncomeExpenseSummary` | split `counts_in_report` | Baris "Selisih tak terlacak" terpisah |
| `DebtItem` | `debt.direction`, `status` | LENT/BORROWED, 4 status, bar terbayar, jatuh tempo |
| `SplitRow` | `transaction_split` | Kategori + nominal + hapus, indikator sisa 0 |
| `GroupOrderShareRow` | `group_order_share` | Nama, rincian, total, status bayar |
| `ReconciliationSummary` | `reconciliation` | Saldo Arus, saldo asli, selisih |
| `ItemOwnerChips` | `item_owner` | Pemilik ganda dengan pratinjau pembagian |

Transfer, pelunasan, dan pinjaman **tidak pernah** memakai gaya pemasukan atau pengeluaran. Pembayaran group order menampilkan nominal total dengan keterangan kecil "laporan: Rp 40.000".

---

## 6. Aturan Layout Global

1. Top bar hanya kembali dan judul.
2. Satu aksi utama per layar sebagai tombol pill di bawah; detail tanpa menu ⋮.
3. Bottom nav hanya di layar akar tab; sub-halaman memakai tombol kembali.
4. Bottom sheet untuk input ringan; dialog untuk konfirmasi dan peringatan.
5. Form: nominal paling atas, keyboard sistem, Simpan terpasang di atas keyboard.
6. Aksi destruktif selalu ada konfirmasi atau Urungkan.
7. Area sentuh ≥ 48dp, semua ikon tombol punya deskripsi.
8. Tanpa kartu bersarang, tanpa pembagi garis kecuali di dalam daftar.

---

## 7. Ikon

- Font Awesome untuk **semua** ikon, termasuk pilihan pengguna di S-41 dan S-21.
- Satu pintu: `ArusIcon(key, size)`; ukuran 16 / 20 / 24dp.
- Database menyimpan **kunci string stabil** (kolom `icon` di ERD), bukan codepoint. Registry kunci ke glyph ada di satu tempat, dengan ikon cadangan untuk kunci yang tidak dikenal.
- Pisahkan ikon UI dan ikon kategori; hanya ikon kategori yang boleh dipilih pengguna.
- Gunakan dependensi Font Awesome yang sudah ada di proyek bila ada. Periksa apakah itu Free atau Pro, karena menentukan set ikon yang tersedia.
- Preset dompet memakai avatar inisial berwarna, bukan logo bank.

---

## 8. Fase Kerja

| Fase | Isi | Hasil |
|---|---|---|
| **1. Audit** | Inventaris layar yang ada, bandingkan dengan wireframe (`S-xx`), catat pelanggaran token, duplikasi, ketergantungan, versi Compose dan API | `docs/design-system/UI_AUDIT.md` |
| **2. Fondasi** | Token, tema, font, `AmountFormatter`, `ArusIcon`, `ArusHaptics` | `DESIGN.md`, galeri token |
| **3. Komponen** | Komponen inti, komponen keuangan, ilustrasi awal | Galeri komponen dengan Preview |
| **4. Refactor layar** | Per prioritas di bawah, tanpa mengubah logika | Layar sesuai wireframe |
| **5. QA** | Konsistensi, aksesibilitas, skala font, layar kecil | `docs/design-system/DESIGN_QA.md` |

**Prioritas refactor** (setelah audit memastikan layar mana yang ada):

| Prioritas | Layar |
|---|---|
| P0 | S-10 Beranda, S-31 Form Transaksi, S-30 Riwayat, S-34 Transfer, S-37/S-38 pilih kategori dan dompet |
| P1 | S-33 Detail Transaksi, S-20 sampai S-22 Dompet, S-40/S-41 Kategori, S-50 Anggaran, S-100 sampai S-103 Rekonsiliasi |
| P2 | Group order, utang-piutang, rutin dan reminder, laporan, OCR, backup, pengaturan, onboarding |

Batasan tetap: tidak mengubah business logic, skema database, repository, kontrak API, destinasi navigasi, dan tidak menghapus fungsi. Bila implementasi punya keterbatasan arsitektur, dicatat sebelum diubah.

---

## 9. Risiko dan Pertanyaan Terbuka

1. **Consolas tidak bisa dibundel.** Consolas adalah font milik Microsoft (bawaan Windows dan Office), tidak tersedia di Android, dan lisensinya tidak mengizinkan disematkan ke aplikasi. Usulan: pakai pengganti berlisensi terbuka yang berkarakter mirip, misalnya **Inconsolata** (paling dekat tampilannya) atau **JetBrains Mono** (angka lebih lega). Token tetap bernama `fontMono`, jadi bisa ditukar tanpa mengubah layar.
2. **Biru `#0009FF` di dark mode.** Kontras terhadap latar hitam kira-kira 2,3:1, tidak cukup untuk teks. Saat dark mode dikerjakan perlu varian biru lebih terang untuk teks dan ikon. Tidak memengaruhi light theme.
3. **Font Awesome Free vs Pro.** Free punya set ikon terbatas dan kewajiban atribusi. Perlu dicek apakah semua kategori bawaan punya ikon yang cocok.
4. **Ilustrasi sketsa.** Aku bisa membuat set awal sebagai pengisi, tetapi hasil yang benar-benar hidup biasanya lebih baik digambar manusia.
5. **Tiga font** menambah ukuran aplikasi (kecil, tetapi perlu dicatat). Gunakan hanya berat yang dipakai (mis. Lora Regular/Italic/SemiBold saja).
6. **Aplikasi sudah jadi:** risiko regresi saat refactor. Perlu tangkapan layar sebelum dan sesudah untuk tiap layar prioritas.
7. Aku belum membuka halaman warna Tailwind dan belum melihat notino.so; nilai zinc di atas dari pengetahuanku (skala v3).
