# Arus: Audit UI dan Design System

**Tanggal pembaruan:** 7 November 2026
**Status:** Compile, 29 unit test, lint (0 error, 66 warning, 18 hint), dan 117 instrumented test di AVD lulus 100%; audit visual dan aksesibilitas belum lengkap
**Acuan:** `docs/design-system/arus-design-system-plan-v2.md`, `arus-user-flow.md`, `arus-erd.md`, dan wireframe `S-xx`

---

## Cakupan

Audit ini memeriksa source Kotlin/Compose untuk arsitektur, routing, komponen, token, resource font, dan alur yang terlihat di kode. Status “Ada” berarti implementasi source ditemukan, bukan bukti kesesuaian visual, aksesibilitas hasil render, atau kelulusan pengujian.

Nomor baris sengaja tidak dicantumkan karena berubah selama refactor. Cari nama file dan simbol yang disebut.

## 1. Arsitektur dan navigasi

### 1.1 Modul dan Compose

- Proyek memakai satu modul aplikasi, `:app`. `app/build.gradle.kts` menetapkan `compileSdk = 37`, `targetSdk = 37`, dan `minSdk = 26`.
- Dependency app memakai Compose UI/Foundation, Animation, dan Paging Compose. Material 3 serta Navigation Compose tidak tercantum; UI memakai komponen Foundation kustom.
- Implementasi baru dan komponen lama masih hidup berdampingan. Ekstraksi layar belum berarti seluruh layar memakai design system baru.

### 1.2 Routing dan shell

- `MainActivity` mengelola `selectedDestination` dengan conditional dispatch. Routing bukan `NavHost`.
- Bottom navigation menyediakan Beranda, Transaksi, Anggaran, dan Lainnya, dengan FAB tengah. Transaksi membuka Riwayat. `QuickAddActionSheet` memuat pengeluaran, pemasukan, transfer, scan struk, group order, dan cocokkan saldo.
- Bottom navigation tetap tampil pada rute subfitur. Tombol kembali tambahan pada Anggaran sudah dihapus; Laporan masih memakai tombol kembali terpisah. Pola ini belum sesuai ketentuan plan bahwa subhalaman memakai tombol kembali tanpa bottom nav.
- Layar modular Beranda, Transaksi/Riwayat, Form Transaksi, Detail Transaksi, Transfer, Dompet, Kategori, dan Anggaran aktif dirutekan. Composable legacy `WalletManagement`, `CategoryManagement`, `HomeOverview`, `TransactionHistory`, `TransactionDetails`, `TransferEntry`, `TransactionQuickEntry`, serta dialog form pembantunya telah dihapus dari `MainActivity`.

### 1.3 State dan akses data

- Layar hasil ekstraksi membaca DAO melalui coroutine dan `LaunchedEffect`; `MainActivity` mengelola state navigasi serta overlay. Router menyimpan ID target edit dompet/kategori dengan `rememberSaveable`, lalu memuat entitas di dispatcher IO sebelum membuka form. Penyimpanan ID ini tidak menjamin draft input bertahan setelah activity recreation; cakupan siklus hidup dan performa layar lain belum diperiksa.
- Pertahankan kontrak callback, DAO, dan perilaku bisnis saat refactor visual. Perubahan routing terbaru belum mengubah skema Room.

## 2. Status design system

### 2.1 Fondasi yang ditemukan

- `ArusTokens.kt` mendefinisikan warna brand `#0009FF`, skala zinc, warna semantik, radius, dan spacing.
- `ArusTheme.kt` memuat Plus Jakarta Sans, Lora, serta JetBrains Mono. Resource font berada di `app/src/main/res/font/`.
- `ArusIcon.kt` menyediakan registry ikon Font Awesome berbasis string key dan alias ikon lama. Overload `ArusIconCode` masih tersedia.
- Komponen reusable mencakup button, input, card, sheet, dialog, navigation, `AmountText`, `TransactionItem`, `WalletCard`, `BudgetProgress`, empty/error state, dan preview gallery.
- `ArusHaptics.kt` menyediakan wrapper haptic. Source wrapper membaca preferensi `haptics_enabled`; audit belum menemukan kontrol Settings yang menulis preferensi tersebut. `springPress` memeriksa `ANIMATOR_DURATION_SCALE`; cakupan animasi lain belum diverifikasi.

### 2.2 Ketidakkonsistenan yang terverifikasi di source

- Komponen legacy di `ArusComponents.kt` memakai ukuran minimum berbeda dari komponen inti: button 44dp, text field 42dp, checkbox 24dp; dropdown menu item dan navigation item tidak menetapkan batas minimum ukuran. Komponen baru menyediakan minimum 48dp untuk kontrol umum.
- `ArusSegmentedControl` memiliki opsi klik setinggi 40dp. Aksi cepat di `HomeScreen` memakai tinggi 42dp. Teks “Lihat semua” memakai `.clickable` tanpa ukuran target eksplisit.
- `ArusCard` yang dapat diklik tidak memaksakan ukuran target minimum. Pemeriksaan source belum membuktikan ukuran sentuh hasil render.
- `ArusSwitch` memiliki animasi terpisah dari `springPress`; dukungan reduce motion untuk animasi switch belum diverifikasi.
- `HomeScreen` membuka rincian kekayaan bersih lewat dialog lokal. Callback `onNetWorthDetail` tidak terhubung ke router.
- Satu resource Plus Jakarta Sans dipetakan ke beberapa weight di `ArusTheme.kt`; file font weight terpisah belum ditemukan dalam daftar resource.
- `ArusEmptyState` memakai ikon Font Awesome dan tipografi Lora. Aset ilustrasi sketsa marker belum ditemukan di resource/source yang diperiksa.
- `DESIGN_QA.md` mencatat hasil kontras, touch target, dan status PASS historis. Audit ini belum mengukur ulang kontras atau menguji layar secara visual, sehingga tidak mengesahkan klaim tersebut.

## 3. Inventaris implementasi berdasarkan kode

Status berikut menilai struktur dan keterjangkauan source. Kesesuaian visual wireframe belum dinilai.

| Kode / fitur | Status kode | Implementasi dan catatan |
|---|---|---|
| S-00a Quick Add | Ada | FAB membuka `QuickAddActionSheet` dengan enam aksi. Beranda juga menampilkan baris aksi cepat; periksa hierarki dan duplikasi lewat screenshot. |
| S-10 Beranda | Ada, aktif | `HomeScreen` dirutekan dari `MainActivity`; menampilkan ringkasan, carousel dompet, aksi cepat, dan aktivitas. Composable legacy `HomeOverview` telah dihapus dari `MainActivity`. Visual belum diverifikasi. |
| S-11 Rincian kekayaan bersih | Parsial | Rincian aset dan kewajiban muncul dalam dialog, bukan layar penuh. |
| S-12 Tugas tertunda / inbox | Inbox terpusat belum ditemukan | Occurrence rutin tertunda tampil di `RecurringManagement`; routing tidak memiliki tujuan inbox khusus. |
| S-20–23 Dompet | Screen modular dirutekan | `WalletListScreen` dibuka dari “Lihat semua” di Beranda; kartu dompet membuka detail langsung. Daftar membuka form tambah/edit dan detail; detail mendukung rekonsiliasi, transaksi, dan edit. Router menyimpan ID target edit untuk pemulihan setelah activity recreation. Composable `WalletManagement` lama telah dihapus dari `MainActivity`. Tes AVD mencakup detail dompet terarsip dan edit setelah activity recreation; belum mencakup seluruh aksi daftar/detail modular. |
| S-30 Riwayat, S-36 cari/filter | Ada, aktif | `TransactionHistoryScreen` dirutekan; pencarian dan filter berada di layar Riwayat. Composable legacy `TransactionHistory` telah dihapus dari `MainActivity`. |
| S-31 Form transaksi | Ada, aktif | `TransactionFormScreen` ditampilkan sebagai form transaksi. Composable legacy `TransactionQuickEntry` telah dihapus dari `MainActivity`. |
| S-32 / S-32a Split kategori | Parsial | Split berada di dalam alur form transaksi; sheet terpisah belum ditemukan pada implementasi yang diperiksa. |
| S-33 Detail transaksi | Ada, aktif | `TransactionDetailScreen` menampilkan detail dan konfirmasi hapus. Composable legacy `TransactionDetails` telah dihapus dari `MainActivity`. |
| S-34 Transfer | Ada, aktif | `TransferScreen` dibuka dari state overlay; pemilihan dompet memakai picker. Composable legacy `TransferEntry` telah dihapus dari `MainActivity`. |
| S-35 Template transfer | Parsial / alur belum diverifikasi | Source memuat “Tarik tunai”, “Bayar kredit”, dan “Top up”. Keberadaan pilihan belum membuktikan hasil setiap alur sesuai wireframe. |
| S-37 / S-38 Picker kategori/dompet | Ada sebagai bottom sheet | `CategoryPickerSheet` dan `WalletPickerSheet` tersedia; perilaku visual belum diuji. |
| S-40–42 Kategori | Screen modular dirutekan | `Kategori` dibuka dari Lainnya. `CategoryListScreen` membuka `CategoryFormScreen`, menyediakan aksi naik urutan, dan menyegarkan daftar setelah mutasi; `DeleteCategoryDialog` menangani migrasi transaksi dan transaksi split secara atomik atau pembersihan referensi tanpa kategori. Router menyimpan ID target edit setelah activity recreation. Composable `CategoryManagement` lama telah dihapus dari `MainActivity`. Tes regresi migrasi transaksi/split dan activity recreation ditambahkan ke suite instrumented test. |
| S-50–51 Anggaran | Screen modular dirutekan | Tab Anggaran membuka `BudgetScreen`; tombol tambah mengganti daftar dengan `BudgetFormScreen`. Refresh list dipicu setelah penyimpanan. Progress dimuat di dispatcher IO; rincian sisa rollover dan seluruh transaksi periode tetap tampil. Layar `BudgetManagement` lama masih ada, tetapi bukan rute aktif Anggaran. Tes AVD mencakup buka/tutup form, simpan anggaran, dan refresh daftar; tampilan rollover serta seluruh transaksi belum memiliki tes UI khusus. |
| S-52–53 Laporan | Ada di routing | `ReportScreen` dirutekan dari menu Lainnya. Detail visual belum diperiksa. |
| S-60–63 Rutin | Ada di routing | `RecurringManagement` memuat daftar, occurrence tertunda, dan pengingat. Inbox terpusat S-12 belum ditemukan. |
| S-70–75 Utang-piutang | Ada di routing; form kontak mandiri belum ditemukan | `DebtManagement` dirutekan. Kontak dapat dibuat lewat input nama pada pembuatan utang atau peserta Group Order. |
| S-80–87 Group Order | Ada di routing; S-83a parsial | Input item, harga, jumlah, pemilik, dan tambah item tampil inline pada langkah item; sheet terpisah belum ditemukan. Pemilih tanggal S-81 perlu verifikasi fungsional. |
| S-90–93 OCR | Implementasi tersedia; belum diuji | `ReceiptScanner` dan importer Group Order tersedia. Audit ini tidak menguji izin kamera/galeri, crop, OCR, atau handoff. |
| S-100–103 Rekonsiliasi | Ada dalam satu alur | `ReconciliationScreen` dibuka dari state rekonsiliasi. Alur memuat saldo aktual, hitung uang tunai, pencocokan mutasi, dan resolusi selisih. Tahapan bukan rute terpisah. |
| S-104 Riwayat rekonsiliasi | Ada | `CheckpointHistory` dibuka dari Riwayat. Source memuat card bersarang pada ringkasan bulanan; cek screenshot dan pertimbangkan perataan hierarki. |
| S-110–114 Pengaturan, keamanan, backup, ekspor, izin | Implementasi tersebar | Pengaturan, keamanan, backup, dan ekspor tersedia pada modul/rute terpisah. Pemetaan izin/notifikasi ke wireframe belum lengkap. |
| S-01–06 Kunci dan onboarding | Implementasi tersedia; audit visual belum dilakukan | Startup, onboarding, PIN/biometrik, dan pemulihan berada di `MainActivity` serta modul terkait. Kesesuaian tiap langkah dengan wireframe belum diverifikasi. |

## 4. Isu dan pekerjaan tersisa

### Prioritas: konsistensi navigasi dan komponen

1. Tentukan kebijakan bottom navigation untuk subhalaman; shell saat ini tetap menampilkannya.
2. Audit dan migrasikan komponen legacy yang memiliki target sentuh di bawah 48dp atau tanpa ukuran minimum.
3. Tinjau aksi cepat Beranda bersama FAB/sheet melalui screenshot AVD.
4. Hapus implementasi layar duplikat hanya setelah seluruh perilaku dan rute lama diverifikasi.

### Fitur belum lengkap atau belum terbukti

- S-12: inbox terpusat belum ditemukan.
- S-70a: form kontak mandiri belum ditemukan.
- S-83a: sheet item Group Order belum ditemukan; input item masih inline.
- S-35: alur end-to-end tiga template transfer belum diverifikasi.
- Pemilih tanggal S-81 perlu verifikasi.
- Ilustrasi marker belum ditemukan.
- Dukungan reduce motion pada semua animasi belum diverifikasi; kontrol preferensi haptic juga belum ditemukan.
- S-11 masih dialog, bukan layar penuh.

### QA yang masih diperlukan

- Build dan 29 unit test lulus. Lint tidak menemukan error, tetapi masih mencatat 66 warning dan 18 hint; task lulus tidak berarti bebas temuan lint.
- Instrumented test AVD lulus 117 dari 117 test (0 skipped, 0 failed) pada AVD `arus_api26` (`emulator-5554`, Android 8.0.0). Tidak ada pengujian pada perangkat fisik.
- Suite pengujian mencakup navigasi modular (Dompet, Kategori, Anggaran), pemulihan ID edit setelah activity recreation, aksi naik urutan kategori, rincian rollover, seluruh transaksi anggaran, pemindahan query dari UI thread, serta verifikasi integritas referensi transaksi dan split pada penghapusan kategori.
- Ambil screenshot AVD untuk Beranda/FAB, Riwayat/filter, form transaksi/split, detail transaksi, transfer, rekonsiliasi, Dompet, Kategori, dan Anggaran.
- Uji TalkBack, font scale, kontras teks/ikon, fokus, dan ukuran target sentuh hasil render.
- Cocokkan setiap status S-xx dengan wireframe aktual. Total file wireframe dan kelengkapan matriks belum dihitung ulang.

## 5. Status rencana v2

| Fase | Status berdasarkan source dan pemeriksaan ini |
|---|---|
| 1. Audit | Audit source diperbarui. Validasi visual dan aksesibilitas perangkat belum selesai. |
| 2. Fondasi | Token, tema, resource font, registry ikon, haptic wrapper, dan formatter nominal tersedia. Adopsi lintas layar dan aksesibilitas belum konsisten. |
| 3. Komponen | Komponen reusable dan preview gallery tersedia. Komponen legacy masih ada. |
| 4. Refactor layar | Screen modular Beranda, Transaksi/Riwayat, Form Transaksi, Detail Transaksi, Transfer, Dompet, Kategori, dan Anggaran aktif dirutekan. Composable legacy `WalletManagement`, `CategoryManagement`, `HomeOverview`, `TransactionHistory`, `TransactionDetails`, `TransferEntry`, dan `TransactionQuickEntry` telah dibersihkan dari `MainActivity`; cakupan refactor layar sekunder (Laporan, Rutin, Utang) berlanjut. |
| 5. QA | `compileDebugKotlin`, `testDebugUnitTest` (29 test), `lintDebug` (0 error), dan `connectedDebugAndroidTest` (117 test di AVD `arus_api26`) lulus 100%. Screenshot AVD, TalkBack, font scale, serta pengukuran kontras belum dicatat. |

Rujukan desain tetap `docs/design-system/arus-design-system-plan-v2.md`. Status source tidak menyatakan layar siap produksi; validasi visual dan QA perangkat tetap diperlukan.
