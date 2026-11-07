# ARUS Design System (DESIGN.md)

**Versi:** 2.0  
**Status:** Sumber Kebenaran Tunggal (*Single Source of Truth*)  
**Platform:** Android Native (Jetpack Compose Foundation)  
**Kategori:** Pencatatan Keuangan Pribadi  

---

## 1. Identitas & Karakter Produk

> *"Less noise. More control."*

- **Karakter:** Hitam-putih, minimal, modern, **playful**, ramah.
- **Fondasi Visual:** Monokrom bersih berbasis Tailwind Zinc, dengan satu aksen tegas **Electric Blue (`#0009FF`)** yang digunakan hemat dan bertujuan (CTA utama, tab aktif, item terpilih, cincin fokus).
- **Nuansa Playful:** Sentuhan haptic interaktif yang presisi di setiap aksi fisik, transisi mikro pegas (spring motion), dan ilustrasi sketsa spidol ujung bulat dengan isian biru geser pada momen emosional (onboarding, empty state, sukses, kunci).
- **Hierarki Keuangan:** Angka dan status uang adalah subjek terpenting. Nominal pengeluaran jelas bertanda minus, pemasukan hijau tenang, angka di daftar dan tabel tabular sejajar (monospace).

---

## 2. Token Desain

### 2.1 Warna

#### Brand & Aksen
| Token | Nilai Hex | Peran & Penggunaan | Kontras |
|---|---|---|---|
| `primary` | `#0009FF` | CTA utama, indicator nav aktif, status terpilih | ≈ 8.5:1 terhadap onPrimary |
| `onPrimary` | `#FFFFFF` | Teks dan glyph pada permukaan primary | - |
| `primaryPressed` | `#0007CC` | Status tombol ditekan (-12% brightness) | - |
| `primarySubtle` | `#EEF0FF` | Tint sangat muda untuk badge kecil dan cincin fokus (**bukan** latar kartu) | - |

#### Netral (Tailwind Zinc)
| Skala | Hex | Token Arus | Penggunaan |
|---|---|---|---|
| Zinc 50 | `#FAFAFA` | `surfaceSubtle` | Latar selang-seling baris / chip sekunder |
| Zinc 100 | `#F4F4F5` | `surfaceSecondary` | Latar input, container segmen, pill nonaktif |
| Zinc 200 | `#E4E4E7` | `border`, `surfaceTertiary` | Garis batas elemen, pemisah daftar |
| Zinc 300 | `#D4D4D8` | `borderStrong` | Garis batas elemen saat disentuh / aktif |
| Zinc 400 | `#A1A1AA` | `textMuted` | Placeholder dan keterangan non-esensial |
| Zinc 500 | `#71717A` | `textSecondary` | Label sekunder, tanggal, tipe dompet (≥ 4.5:1) |
| Zinc 600 | `#52525B` | `iconSecondary` | Ikon pelengkap, chevron navigasi |
| Zinc 800 | `#27272A` | `surfaceDark` | Latar gelap sekunder |
| Zinc 900 | `#18181B` | `inverseSurface` | Latar kartu sorotan Net Worth |
| Zinc 950 | `#09090B` | `textPrimary` | Teks utama, judul, nominal pengeluaran |

- `background` = `#FFFFFF` (atau `#FAFAFA` untuk screen background)
- `surface` = `#FFFFFF` (permukaan kartu dan sheet)

> [!IMPORTANT]
> **Aturan Biru:** Biru `#0009FF` tidak pernah menjadi latar kartu hero atau kartu saldo! Kartu sorotan Net Worth selalu memakai permukaan gelap `inverseSurface` (`#18181B`).

#### Semantik Keuangan
| Token | Nilai Hex | Peran & Penggunaan |
|---|---|---|
| `income` | `#16A34A` | Ikon, bar pemasukan, indikator positif |
| `incomeText` | `#15803D` | Teks nominal pemasukan (rasio kontras 5:1 pada putih) |
| `expense` | `#DC2626` | Selisih minus, saldo defisit, budget terlampaui |
| `expenseSurface` | `#FEF2F2` | Latar peringatan error / batas terlampaui |
| `warning` | `#D97706` | Indikator progres budget 80%, jatuh tempo dekat |
| `warningText` | `#B45309` | Teks peringatan jatuh tempo (≥ 4.5:1) |
| `info` | `#0009FF` | Sama dengan primary |

#### Aturan Nominal Keuangan
- **Pengeluaran:** Teks utama (`textPrimary`) dengan tanda minus: `-Rp 45.000`.
- **Pemasukan:** Teks `incomeText` dengan tanda plus: `+Rp 1.500.000`.
- **Transfer, Pelunasan, Pinjaman:** Teks `textSecondary`: `Rp 500.000` (tanpa tanda +/- dan tidak pernah berwarna hijau/merah).
- **Merah (`expense`):** Dicadangkan secara eksklusif untuk **selisih rekonsiliasi**, **saldo minus**, dan **budget terlampaui**.

---

### 2.2 Tipografi

| Peran | Keluarga Font | Ukuran / Spasi Baris | Berat | Penggunaan |
|---|---|---|---|---|
| **Display** | Plus Jakarta Sans | 32sp / 40sp | Bold (700) | Net worth hero, input nominal besar |
| **Heading Large** | Plus Jakarta Sans | 24sp / 32sp | Bold (700) | Judul layar utama, onboarding |
| **Heading Medium** | Plus Jakarta Sans | 20sp / 28sp | SemiBold (600) | Judul top bar, judul seksi |
| **Heading Small** | Plus Jakarta Sans | 18sp / 24sp | SemiBold (600) | Judul kartu, modal title |
| **Body Large** | Plus Jakarta Sans | 16sp / 24sp | Normal (400) | Isi teks utama, input teks |
| **Body Medium** | Plus Jakarta Sans | 14sp / 20sp | Normal (400) | Isi sekunder, deskripsi |
| **Body Small** | Plus Jakarta Sans | 12sp / 16sp | Normal (400) | Metadata, tanggal transaksi, hint |
| **Label** | Plus Jakarta Sans | 11sp / 16sp | SemiBold (600) | Badge status, tag seksi uppercase |
| **Editorial** | Lora | 24sp / 32sp | SemiBold (600) | Tagline onboarding, headline empty state, ucapan sukses |
| **Editorial Quote**| Lora | 16sp / 24sp | Italic (400) | Kalimat insight finansial |
| **Amount Mono** | JetBrains Mono | 16sp / 22sp | Medium (500) | Nominal di daftar riwayat, rincian per orang di group order, tabel mutasi |
| **Code** | JetBrains Mono | 14sp / 20sp | Normal (400) | Nomor rekening, nomor telepon di tagihan, token, keypad PIN |

#### Aturan Penggunaan Font:
1. **Lora** hanya untuk teks pendek yang membawa sentuhan emosional (onboarding, empty state, success state). Tidak boleh digunakan untuk tombol, field, angka, atau label navigasi.
2. **JetBrains Mono** digunakan ketika keteraturan digit (rata kanan/tabular) penting untuk perbandingan cepat.
3. Tidak ada font lain di luar ketiga font ini.

---

### 2.3 Bentuk (Shapes), Spasi & Elevasi

#### Bentuk
- `pill`: Bulat penuh (`CircleShape`) — digunakan untuk:
  - Tombol aksi (`ArusPrimaryButton`, `ArusSecondaryButton`, `ArusTertiaryButton`)
  - Filter chip & pilihan chip
  - Segmented control indicator
  - Badge status
  - Indikator tab navigasi
- `input`: `18dp` — untuk seluruh text field, search field, dan input form.
- `card`: `24dp` — untuk kartu sorotan Net worth, kartu dompet, dan kartu data.
- `sheet` & `dialog`: `28dp` — sudut membulat untuk bottom sheet dan dialog konfirmasi.

#### Spasi
Skala spasi wajib kelipatan sistem 4dp:  
`4dp`, `8dp`, `12dp`, `16dp`, `20dp`, `24dp`, `32dp`, `40dp`.

- **Padding Layar:** 16dp horizontal.
- **Jarak Antarseksi:** 24dp.
- **Jarak Antarkartu:** 12dp.
- **Padding Dalam Kartu:** 16dp.

#### Elevasi & Garis Batas
- **Tanpa Bayangan (Zero Elevation):** Tidak ada *drop shadow* kabur (blur).
- Elemen dipisahkan secara tegas melalui:
  - Garis batas 1dp (`BorderStroke(1.dp, ArusTheme.colors.border)`).
  - Kontras latar belakang (`surface` vs `surfaceSecondary`).
  - Spasi ruang putih yang lega.
- **Pengecualian:** Bottom navigation bar melayang (*floating pill*) dapat memiliki elevasi halus atau border pembatas tegas.

---

## 3. Sistem Interaksi & Playful

### 3.1 Haptic Feedback (`ArusHaptics`)
Semua pemanggilan haptic wajib melalui satu pintu: `ArusHaptics`. Layar dilarang memanggil `HapticFeedback` atau `Vibrator` Android secara langsung.

| Peristiwa | Pola Haptic | Kompatibilitas |
|---|---|---|
| **Ketuk Tombol Utama** | Klik ringan (`CONFIRM` / `CLOCK_TICK`) | Fallback short tick |
| **Ganti Tab / Chip / Keypad PIN** | Tick pendek (`CONTEXT_CLICK` / `KEYBOARD_TAP`) | Tick ringan |
| **Saklar Hidup / Mati** | Dua getaran berurutan naik/turun | Pola 15ms vs 30ms |
| **Simpan Berhasil / Saldo Cocok** | Konfirmasi ritmis (*success double pulse*) | Dua pulsa 20ms |
| **Validasi Gagal / Batas Terlampaui** | Penolakan (*reject buzzing*) | Tiga getaran cepat |
| **Aksi Destruktif (Hapus)** | Peringatan (*warning heavy pulse*) | Pulsa panjang 40ms |

*Semua haptic menghormati saklar di Pengaturan Arus dan setelan OS pengguna.*

### 3.2 Gerakan (Spring Motion)
- **Tekan (Press):** Tombol dan kartu interaktif mengecil ±3% (`scale(0.97f)`) saat ditekan dengan animasi pegas kenyal (`spring(dampingRatio = 0.7f, stiffness = 400f)`).
- **Tombol (+):** Memantul ringan saat bottom sheet muncul.
- **Keberhasilan:** Centang menggambar diri (*path morph*) saat checkpoint tersimpan.
- **Aksesibilitas:** Semua animasi pegas otomatis dimatikan menjadi instan saat setelan sistem "Kurangi Animasi" (*prefers-reduced-motion*) aktif.
- **Durasi Maksimum:** Transisi fungsional tidak pernah melebihi 300ms.

### 3.3 Ikonografi (`ArusIcon`)
- Ikon menggunakan **Font Awesome 6 Free Solid** yang tersemat di `res/font/font_awesome_solid.ttf`.
- Pemanggilan terpusat: `ArusIcon(key: String, modifier: Modifier, size: Dp, contentDescription: String?, tint: Color)`.
- Database Room menyimpan **string key stabil** (misal: `"wallet"`, `"utensils"`, `"money-bill"`).
- Registry memiliki fallback otomatis untuk ikon yang tidak dikenal (`circle-question`).
- Preset dompet (BCA, Mandiri, Seabank, GoPay) menggunakan **avatar inisial berwarna** berbentuk pill/lingkaran dengan font SemiBold, bukan logo bank luar.

---

## 4. Aturan Tata Letak Global

1. **Top Bar Sederhana (`ArusTopBar`):** Hanya berisi tombol kembali (`←`) dan judul layar. **Tanpa aksi tombol atau menu di pojok kanan.**
2. **Satu Aksi Utama per Layar:** Tombol pill hitam penuh (`ArusPrimaryButton`) terpasang di bagian bawah layar. Layar detail tidak memiliki menu titik tiga (`⋮`).
3. **Bottom Navigation 4 Tab + Center FAB (+):**
   - Tab 1: **Beranda** (`house`)
   - Tab 2: **Transaksi** (`clock`)
   - Tengah: **Tombol (+) Melayang** (`plus`) — membuka bottom sheet aksi cepat 6 item (`S-00a`).
   - Tab 3: **Anggaran** (`piggy-bank`)
   - Tab 4: **Lainnya** (`ellipsis`)
   - Sub-halaman menyembunyikan bottom bar dan menggunakan tombol kembali di top bar.
4. **Formulir Keuangan:**
   - Nominal uang ditaruh di posisi paling atas dengan ukuran Display.
   - Menggunakan keyboard sistem bawaan.
   - Tombol "Simpan" terpasang di atas keyboard (*IME padding*).
5. **Modalitas:**
   - Gunakan `ArusBottomSheet` (radius 28dp) untuk pilihan, pemilih dompet/kategori, dan input cepat.
   - Gunakan `ArusDialog` (radius 28dp) khusus untuk konfirmasi kritis dan peringatan saldo.
   - Dilarang membuat dialog bersarang (dialog di atas dialog).
6. **Aksesibilitas & Area Sentuh:**
   - Seluruh elemen sentuh (tombol, chip, checkbox, ikon baris) memiliki area sentuh minimum **≥ 48×48dp**.
   - Setiap tombol ikon wajib memiliki `contentDescription` deskriptif.

---

## 5. Komponen Inti & Keuangan

### Komponen Inti
- `ArusPrimaryButton`: Pill hitam penuh, teks putih, haptic klik, skala spring saat ditekan.
- `ArusSecondaryButton`: Pill outline border `borderStrong`, teks `textPrimary`.
- `ArusTertiaryButton`: Pill transparan (ghost), teks `textSecondary`.
- `ArusIconButton`: Lingkaran minimum 48×48dp, ikon FA 20dp/24dp.
- `ArusCard`: Sudut 24dp, border 1dp, tanpa bayangan.
- `ArusBalanceCard`: Kartu sorotan gelap (`#18181B`), teks putih, chip ringkasan aset/kewajiban.
- `ArusTextField` & `ArusSearchField`: Sudut 18dp, tinggi minimum 48dp, placeholder `textMuted`.
- `ArusAmountInput`: Nominal besar 32sp, format real-time otomatis.
- `ArusChip` & `ArusFilterChip`: Pill, aktif berlatar hitam/biru dengan teks putih.
- `ArusSegmentedControl`: Container rounded pill berlatar `surfaceSecondary`, pill aktif putih/hitam.
- `ArusSwitch`: Saklar haptic dengan warna aktif `#0009FF`.
- `ArusBadge`: Pill kecil dengan tipografi Label 11sp SemiBold.
- `ArusTopBar`: Tinggi 56dp, tombol kembali pill 48dp, judul Heading Medium.
- `ArusBottomNavigation`: Bar melayang dengan 4 tab + 1 FAB center bulat.
- `ArusBottomSheet`: Permukaan putih sudut atas 28dp, pill handle di atas.
- `ArusDialog`: Modal dialog sudut 28dp dengan pesan konfirmasi dan dua aksi.
- `ArusSnackbar`: Feedback mengambang dengan tombol "Urungkan" 5 detik.
- `ArusEmptyState` & `ArusErrorState`: Ilustrasi sketsa spidol, judul Lora 24sp, tombol CTA pill.

### Komponen Keuangan
- `AmountText`: Satu-satunya pemformat nominal rupiah (`Rp 1.250.000`, negatif `-Rp 65.000`, ringkas `1,2 jt`), font Mono di daftar.
- `TransactionItem`: Menangani 12 tipe transaksi ERD dengan ikon kategori, label tipe, tanggal, dan format nominal yang tepat.
- `WalletCard`: Menampilkan saldo, tipe dompet, dan bar limit kredit jika tipe `CREDIT`.
- `WalletCarousel`: Deretan horizontal dompet dengan kartu berikutnya sedikit terintip.
- `CategoryItem`: Ikon dalam lingkaran warna pastel, nama kategori, dan status sistem.
- `BudgetProgress`: Meteran anggaran dengan status Hijau (normal), Kuning (80%+), Merah (terlampaui).
- `DebtItem`: Dua arah (Piutang `LENT` vs Utang `BORROWED`), bar progres terbayar, dan label jatuh tempo.

---

## 6. Checklist Kepatuhan (*Delivery Gate*)

Sebelum kode UI dinyatakan selesai:
- [ ] Tidak ada warna hex mentah di berkas Composable (semua via `ArusTheme.colors`).
- [ ] Tidak ada `RoundedCornerShape(X.dp)` acak (semua via `ArusTheme.shapes`).
- [ ] Tidak ada `fontSize = X.sp` mentah (semua via `ArusTheme.typography`).
- [ ] Seluruh nominal rupiah ditampilkan melalui `AmountText`.
- [ ] Seluruh ikon ditampilkan melalui `ArusIcon` dengan deskripsi konten aksesibel.
- [ ] Seluruh interaksi sentuh memiliki feedback haptic via `ArusHaptics`.
- [ ] Seluruh area sentuh memenuhi ambang batas ≥ 48×48dp.
- [ ] Tidak ada dialog bersarang (*nested dialogs*) dan tidak ada kartu bersarang (*nested cards*).
- [ ] Kontras teks terhadap latar belakang memenuhi standar WCAG AA (≥ 4.5:1).
- [ ] Seluruh komponen memiliki Preview Compose yang dapat dirender di IDE.

