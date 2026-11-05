# Design

## Context

Repositori belum memiliki aplikasi, modul, tes, atau konfigurasi build; hanya ada `arus-erd.md`, `arus-user-flow.md`, dan wireframe HTML berukuran layar Android. Rencana fase dan perilaku produk dirinci pada `proposal.md` serta delapan delta spec. Dokumen domain menetapkan Android/Room dan komputasi saldo dari transaksi sebagai arah teknis, tetapi baseline teknis belum dapat diverifikasi dari kode.

## Goals / Non-Goals

**Goals:**
- Menetapkan fondasi yang membuat saldo, laporan, dan jejak perubahan konsisten sejak awal.
- Membagi implementasi ke fase yang bisa diuji dan dirilis; fase lanjutan tidak memblokir MVP.
- Menggunakan wireframe S-xx sebagai referensi desain interaksi tanpa menganggap data contoh wireframe sebagai data produksi.

**Non-Goals:**
- Menentukan desain visual baru atau mengimplementasikan layar dalam perubahan ini.
- Mengaktifkan integrasi bank, sinkronisasi akun, atau server.
- Menyelesaikan OCR, cloud backup, budget, utang, maupun group order sebagai bagian MVP.

## Decisions

1. **Bangun Android native secara lokal-first.** Mulai dengan Kotlin dan Jetpack Compose sesuai arah dokumentasi. Alternatif Flutter atau UI berbasis web ditunda karena akan menyimpang dari asumsi platform, notifikasi, kamera, dan penyimpanan lokal yang sudah tertulis. Baseline awal: `minSdk 26` (Android 8.0), package ID `com.illustravibes.arus`, dan UI Bahasa Indonesia. Tetap uji instalasi serta alur inti di perangkat fisik pengguna yang menjalankan versi One UI terbaru; dukungan minSdk tidak membatasi pengujian pada perangkat modern.
2. **Gunakan Room dengan domain/use case terpisah dari UI.** Room menjadi sumber kebenaran lokal; repository/domain menghitung saldo dan laporan melalui query agregat/Flow alih-alih menyimpan saldo turunan atau memuat semua transaksi. Alternatif menyimpan saldo cache ditolak untuk mencegah saldo tidak cocok dengan ledger. Semua operasi transfer, rekonsiliasi, dan finalisasi multi-entitas harus satu transaksi database.
3. **Mulai skema dari domain ERD dan migrasi berversi.** Gunakan enum tersimpan sebagai teks, nominal Long, foreign key RESTRICT, soft delete untuk entitas bersejarah, dan tes migrasi. Model fase 3/4 hanya ditambahkan saat fase itu dikerjakan; tipe transaksi masa depan dapat dicadangkan di domain tanpa menambah tabel belum terpakai. Keputusan enkripsi SQLCipher perlu divalidasi terhadap pemeliharaan dependensi dan strategi pemulihan kunci sebelum rilis data nyata.
4. **Implementasikan irisan vertikal berurutan.** Urutan MVP: scaffold/build dan tema/navigasi → onboarding serta seed kategori/dompet → transaksi dan transfer → riwayat/detail → beranda/net worth → rekonsiliasi/checkpoint → pengamanan dan hardening. Fase 2: kategori split, budget/laporan, rutin/reminder, pencarian dan riwayat rekonsiliasi. Fase 3: kontak, utang dua arah/pelunasan/netting, lalu wizard group order. Fase 4: lampiran/OCR, backup/restore, ekspor. Setiap fase disertai tes domain dan UI sebelum bergerak ke fase berikutnya.
5. **Jadikan dokumen dan wireframe sebagai input yang ditinjau, bukan kontrak implisit.** Pemetaan S-xx ke layar implementasi harus dilakukan pada setiap irisan vertikal. Beberapa wireframe bernama `Pengeluaran`, `Transfer`, `Main`, `Cocokkan`, dan seterusnya; isi yang tersedia adalah mockup, bukan perilaku teruji. Jika bertentangan, delta spec menjadi kontrak perilaku, user flow menjadi alur, dan ERD menjadi model data.
6. **Jaga batas fase tanpa membangun placeholder yang mengunci desain.** Implementasi MVP menyiapkan enum/struktur yang memang disebut perlu hadir awal, namun tidak membuat layar atau data dummy untuk fitur Fase 2–4. Ini mengurangi kompleksitas dan risiko migrasi dini.
7. **Sediakan fondasi split sejak MVP.** ERD menetapkan `transaction_split` sebagai bagian MVP. Transaksi MVP memakai satu split penuh; pemecahan kategori, budget, dan laporan tetap Fase 2.

## Risks / Trade-offs

- [Risiko] Perangkat fisik dapat memiliki batasan vendor/versi sistem yang tidak terwakili tes emulator → uji smoke dan alur inti langsung di perangkat One UI pengguna selain tes otomatis dan emulator pada API minimum.
- [Risiko] Query agregat saldo/laporan serta zona waktu dapat menimbulkan kesalahan tepi → tes untuk transaksi masa depan, perubahan timezone, soft delete, transaksi terverifikasi, dan nominal besar.
- [Risiko] Penghapusan/undo transaksi dapat bertentangan dengan checkpoint atau operasi terkait → sentralisasi aturan domain dan uji integritas sebelum UI membuka aksi tersebut.
- [Risiko] Wireframe dapat tertinggal dari flow v0.6 → audit cakupan layar dan catat ketidaksesuaian saat implementasi fase terkait; jangan menebak perilaku baru dari mockup.
- [Keputusan keamanan] `FLAG_SECURE` selalu aktif pada seluruh window Arus, termasuk layar autentikasi; screenshot dan preview recent-apps tidak menampilkan konten aplikasi.
- [Trade-off] SQLCipher memberi proteksi data lokal tetapi menambah dependensi dan pertimbangan backup/kunci → validasi dukungan dan pendekatan kunci sebelum data produksi; tetap gunakan enkripsi file backup terpisah.

## Migration Plan

1. Buat proyek Android kosong, CI/build dasar, dan skema Room awal dengan `exportSchema = true`.
2. Implementasikan skema MVP sebagai versi pertama; tes query dan migrasi dengan fixture database.
3. Tambahkan entitas fase berikut lewat migrasi eksplisit, selalu menjaga data lama dan menguji upgrade dari versi sebelumnya.
4. Rilis perubahan skema bersama aplikasi yang mampu membacanya. Jika migrasi gagal, gagalkan upgrade secara aman dan jangan menghapus database pengguna; pemulihan dari backup hanya setelah tersedia di Fase 4.

## Open Questions

- Mekanisme distribusi aplikasi belum ditentukan; tentukan sebelum menyiapkan rilis publik.
- Strategi kunci SQLCipher dan pemulihan lokal perlu ditetapkan sebelum rilis yang menyimpan data nyata.
- Batas fitur keamanan MVP (PIN/biometrik dan FLAG_SECURE opsional) perlu disesuaikan dengan kebijakan produk dan kemampuan perangkat.
