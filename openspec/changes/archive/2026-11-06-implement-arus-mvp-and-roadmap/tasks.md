# Tasks

## 1. Fondasi Proyek Android

- [x] 1.1 Buat proyek Android native Kotlin/Jetpack Compose dengan `minSdk 26`, target/compile SDK terbaru yang stabil saat mulai implementasi, application ID `com.illustravibes.arus`, dan UI Bahasa Indonesia; verifikasi konfigurasi Gradle/JDK, build debug, serta instalasi pada emulator API 26.
- [x] 1.2 Siapkan struktur modul/package untuk UI, domain, dan data serta navigasi dasar sesuai keputusan arsitektur; verifikasi build dan navigasi placeholder berjalan.
- [x] 1.3 Siapkan pipeline tes/build otomatis dan dokumentasikan perintah build/test lokal; verifikasi pipeline menjalankan build debug dan tes unit.

## 2. Fondasi Domain dan Penyimpanan Terlindungi

- [x] 2.1 Implementasikan model domain dan enum transaksi bertipe aman, nominal integer, converter teks, serta basis Room berversi dengan `exportSchema = true`; verifikasi tes mapping dan skema hasil ekspor.
- [x] 2.2 Tambahkan SQLCipher dan mekanisme kunci melalui penyimpanan kunci platform, tanpa menulis kunci ke log atau plaintext; verifikasi database dapat dibuat/dibuka ulang dan tes memastikan kunci salah/gagal tidak merusak database.
- [x] 2.3 Implementasikan DAO/repository saldo berbasis saldo awal dan transaksi aktif sampai waktu sekarang; verifikasi tes transaksi masa depan, transaksi terhapus, transaksi masuk/keluar, serta nominal besar.
- [x] 2.4 Tambahkan tes migrasi Room untuk versi awal dan kebijakan kegagalan aman tanpa destructive fallback; verifikasi migrasi mempertahankan fixture data.

## 3. Onboarding, Dompet, dan Kategori

- [x] 3.1 Implementasikan seed preset kategori dan dompet dari data aset serta alur onboarding sambutan, dompet pertama, pilihan kategori, dan lewati keamanan; verifikasi onboarding mengharuskan setidaknya satu dompet dan menghormati pilihan seed.
- [x] 3.2 Implementasikan daftar, tambah/edit, urutkan, detail, dan arsip dompet termasuk field kredit kondisional; verifikasi tes aturan arsip dan uji UI form tiap tipe dompet.
- [x] 3.3 Implementasikan kategori datar, picker kategori, edit, arsip, hapus/pindah transaksi, serta proteksi kategori sistem; verifikasi tes referensi kategori dan skenario konfirmasi hapus.
- [x] 3.4 Cocokkan layar onboarding/dompet/kategori dengan wireframe terkait dan dokumentasikan selisih yang disengaja; verifikasi melalui pemeriksaan visual di emulator.

## 4. Transaksi dan Transfer

- [x] 4.1 Implementasikan use case pemasukan/pengeluaran dengan default dompet/tanggal, pemilihan kategori, catatan, merchant, dan validasi nominal; verifikasi tes domain untuk saldo minus, nominal nol, serta transaksi masa depan.
- [x] 4.2 Implementasikan form transaksi cepat, keypad nominal, simpan & tambah lagi, dan Undo lima detik; verifikasi uji UI alur tambah serta Undo mengembalikan saldo dan riwayat.
- [x] 4.3 Implementasikan transfer atomik dua dompet dengan biaya admin opsional sebagai pengeluaran terpisah; verifikasi tes transaksi database untuk transfer tanpa biaya, dengan biaya, kegagalan parsial, dan larangan dompet sama.
- [x] 4.4 Implementasikan riwayat berpaging per tanggal, detail transaksi, edit/hapus, serta peringatan untuk transaksi khusus; verifikasi tes paging dengan urutan `(occurred_at, id)` dan tes saldo setelah edit/hapus.
- [x] 4.5 Selaraskan layar transaksi, transfer, pemilih dompet/kategori, detail, dan riwayat dengan wireframe; verifikasi alur UI utama pada emulator.

## 5. Beranda dan Ringkasan

- [x] 5.1 Implementasikan query agregat aset dan kewajiban kredit serta net worth MVP; verifikasi tes dompet `include_in_total=false` dan nilai kredit.
- [x] 5.2 Implementasikan beranda, rincian net worth, dompet horizontal, transaksi terbaru, dan navigasi bottom bar/FAB sesuai wireframe; verifikasi seluruh kartu membuka detail yang benar.
- [x] 5.3 Tambahkan state kosong dan format nominal/tanggal Bahasa Indonesia; verifikasi snapshot/UI test pada data kosong dan data berisi.

## 6. Rekonsiliasi Saldo

- [x] 6.1 Implementasikan alur input saldo aktual dan kalkulasi selisih berdasarkan transaksi efektif; verifikasi tes nilai selisih positif, negatif, dan nol.
- [x] 6.2 Implementasikan resolusi melalui transaksi terlupa, pecahan selisih, saldo awal salah, atau selisih tak terlacak dengan operasi atomik; verifikasi tes saldo akhir sama dengan saldo aktual setelah checkpoint.
- [x] 6.3 Implementasikan checkpoint, penandaan transaksi terverifikasi, peringatan edit/hapus, dan `needs_recheck`; verifikasi tes perubahan transaksi yang sudah diverifikasi.
- [x] 6.4 Cocokkan layar rekonsiliasi dengan wireframe input, hasil, dan resolusi; verifikasi uji UI alur lengkap di emulator.

## 7. Keamanan dan Verifikasi MVP Perangkat

- [x] 7.1 Implementasikan penguncian PIN dan biometrik dengan fallback PIN serta kebijakan auto-lock; verifikasi tes PIN benar/salah, biometrik tersedia/tidak tersedia, dan kembali dari background. Suite instrumentasi lulus pada SM-A546E Android 17/API 37 (60 lulus, 1 dilewati karena biometrik tersedia); fallback biometrik tanpa enrollment lulus pada AVD API 26. Catatan: `task-7.1-device-smoke.md`.
- [x] 7.2 Evaluasi serta terapkan FLAG_SECURE sebagai opsi keamanan MVP sesuai rancangan layar; verifikasi screenshot/recent-apps tidak membocorkan konten ketika opsi aktif. FLAG_SECURE aktif pada build rilis dan nonaktif pada build debug agar screenshot review bisa dilakukan.
- [x] 7.3 Jalankan rangkaian tes unit, integrasi Room, migrasi, dan UI untuk MVP; verifikasi seluruh build dan tes lulus.
- [x] 7.4 Jalankan smoke test instalasi dan alur onboarding, transaksi, transfer, beranda, rekonsiliasi, serta kunci pada perangkat fisik One UI pengguna; dokumentasikan versi OS/perangkat dan hasilnya. Suite fisik SM-A546E Android 17/API 37 lulus; onboarding, transfer tersimpan, dan resolusi rekonsiliasi juga lulus pada tes fokus. Catatan: `task-7.1-device-smoke.md`.

## 8. Fase 2 — Anggaran, Laporan, dan Rutin

- [x] 8.1 Tambahkan fondasi split kategori, `counts_in_report`/`counts_in_budget`, dan migrasi; verifikasi jumlah split sama dengan transaksi melalui tes domain. Fondasi tabel, backfill migrasi 11→12, dan split penuh satu kategori MVP tersedia. Pemecahan split multi-kategori tetap Fase 2.
- [x] 8.2 Implementasikan budget total/kategori, periode, rollover, ambang, progress, dan detail transaksi; verifikasi perhitungan batas 80%/100% serta perubahan periode.
- [x] 8.3 Implementasikan laporan pemasukan/pengeluaran dan filter/periode; verifikasi transfer, pelunasan, dan split di luar laporan tidak dihitung.
- [x] 8.4 Implementasikan aturan rutin, occurrence unik, catch-up, mode otomatis/konfirmasi, inbox, serta reminder; verifikasi idempotensi occurrence dan tidak ada pencatatan diam-diam untuk mode konfirmasi.
- [x] 8.5 Implementasikan pencarian/filter, riwayat checkpoint, notifikasi/izin kontekstual yang termasuk Fase 2; verifikasi filter gabungan dan fallback notifikasi saat izin tidak tersedia.
- [x] 8.6 Selaraskan layar Fase 2 dengan wireframe dan jalankan tes integrasi fase; verifikasi build, tes domain, tes UI, dan review visual. Build serta suite penuh 49/49 lulus. Screenshot terbaru mengonfirmasi Anggaran, Laporan, Riwayat, dan Checkpoint; catatan review: `task-8.6-visual-review.md`.

## 9. Fase 3 — Utang-Piutang dan Group Order

- [x] 9.1 Tambahkan kontak, debt dua arah, repayment allocation, kategori sistem terkait, serta migrasi; verifikasi constraint arah/kontak dan jumlah alokasi tidak melebihi pokok.
- [x] 9.2 Implementasikan pinjaman baru dan catatan lama tanpa origin transaction; verifikasi transaksi dompet hanya dibuat untuk mode dengan dompet.
- [x] 9.3 Implementasikan pelunasan, cicilan, alokasi multi-catatan, selisih kurang/lebih, write-off, dan netting; verifikasi saldo, status utang, laporan, serta batas netting. DAO dan layar Utang-Piutang mendukung alokasi multi-catatan, resolusi kelebihan, write-off, netting, dan dampak net worth; migrasi 15→16 memperbaiki batas trigger netting. Tes unit serta suite fisik lulus (70 lulus, 1 dilewati karena biometrik tersedia).
- [x] 9.4 Implementasikan draft dan wizard group order, pembagian item/biaya/diskon, pembulatan, validasi total, dan finalisasi atomik; verifikasi item tanpa pemilik memblokir finalisasi dan total split seimbang. Wizard menyimpan draft, mendukung pemilik item/patungan, biaya rata/proposional/manual, diskon, penanggung pembulatan, dan finalisasi transaksi/split/piutang atomik. Tes DAO, migrasi, kalkulator, dan UI AVD lulus.
- [x] 9.5 Implementasikan detail order, status peserta, pembatalan sesuai aturan pelunasan, dan berbagi tagihan melalui Android share sheet; verifikasi tidak ada pengiriman otomatis. Detail menampilkan saldo/status peserta, pelunasan memperbarui status order, pembatalan menolak order yang sudah punya alokasi, dan berbagi memakai `Intent.createChooser` tanpa mengirim otomatis. Tes GroupOrderDao dan `GroupOrderShareIntentTest` lulus.
- [x] 9.6 Selaraskan layar Fase 3 dengan wireframe dan jalankan tes integritas end-to-end; verifikasi skenario group order dengan pelunasan sebagian. Review mencakup wizard dan detail peserta; perbedaan desain serta perbaikan visual dicatat di `task-9.6-visual-review.md`. Unit dan 80 tes instrumentasi AVD lulus, termasuk pelunasan sebagian hingga status lunas.

## 10. Fase 4 — OCR dan Portabilitas Data

- [x] 10.1 Implementasikan lampiran foto, kamera/galeri, crop/rotate, dan OCR on-device; verifikasi izin ditolak tetap menyediakan fallback galeri/input manual. Alur CameraX, crop/rotate, OCR lokal, lampiran pending, migrasi, dan fallback lulus; unit serta instrumentasi penuh 88/88 lulus. Catatan: `task-10.1-receipt-scan.md`.
- [x] 10.2 Implementasikan konfirmasi hasil OCR, penanda keyakinan, pemilihan transaksi biasa/group order, serta merchant hint; verifikasi OCR tidak pernah menyimpan tanpa konfirmasi. Form transaksi meminta Simpan eksplisit; lampiran dan merchant hint diperbarui setelah konfirmasi. Group Order menghasilkan draft berisi item OCR belum ditetapkan. Unit dan seluruh 92 tes instrumentasi AVD lulus. Catatan: `task-10.2-ocr-confirmation.md`.
- [x] 10.3 Implementasikan backup terenkripsi melalui Storage Access Framework dan restore dengan validasi kata sandi/versi skema serta konfirmasi overwrite; verifikasi restore skema lebih baru ditolak dan kegagalan tidak menghapus data lokal. File AES-GCM mencakup database SQLCipher dan lampiran; staging/migrasi selesai sebelum swap. Unit dan 93 tes instrumentasi AVD lulus. Catatan: `task-10.3-backup-restore.md`.
- [x] 10.4 Implementasikan ekspor CSV/Excel/PDF untuk rentang tanggal; verifikasi berkas hasil berisi data tepat dan dapat dibagikan melalui sistem. DAO/filter tanggal, CSV, XLSX, PDF multi-halaman, SAF, dan chooser diuji; unit dan 97 tes instrumentasi AVD lulus. Catatan: `task-10.4-export.md`.
- [x] 10.5 Selaraskan layar Fase 4 dengan wireframe dan jalankan tes keamanan/restore end-to-end; verifikasi lampiran ikut backup dan data tetap terbaca sesudah restore. Review mencatat selisih SAF dan filter ekspor dari mockup, memperbaiki tombol onboarding terpotong, dan menguji password salah, skema baru, lampiran dari perangkat baru, serta buka ulang database. Unit dan 97 tes instrumentasi AVD lulus. Catatan: `task-10.5-visual-security-review.md`.

## 11. Integrasi dan Rilis Bertahap

- [x] 11.1 Implementasikan editor split multi-kategori transaksi; verifikasi jumlah split harus sama dengan nominal transaksi dan flag laporan/anggaran tetap terjaga. Editor mendukung nominal/persen; DAO menolak split tidak seimbang secara atomik dan menyimpan flag laporan/anggaran. Unit, DAO, serta UI AVD lulus.
- [x] 11.2 Implementasikan penghitung pecahan uang tunai untuk rekonsiliasi; verifikasi total integer, penyimpanan checkpoint, dan tidak ada transaksi otomatis. Migrasi 19→20 menambahkan tabel pecahan; hitungan terikat checkpoint dan tidak membuat transaksi. Tes migrasi, DAO, serta UI AVD lulus.
- [x] 11.3 Implementasikan pengaturan reminder S-63 dan status izin/notifikasi S-114; verifikasi izin ditolak tidak menyembunyikan occurrence tertunda. Pengguna dapat mengaktifkan reminder rutin, mengatur waktu harian, melihat status izin, serta membuka pengaturan notifikasi/baterai. Tes UI AVD dan `RecurringDaoTest` lulus; occurrence pending tetap ada tanpa izin.
- [x] 11.4 Implementasikan pengaturan umum S-110 untuk tema, awal periode, dan format lokal; verifikasi preferensi bertahan setelah aplikasi dibuka ulang. Tema terang/gelap/sistem, tanggal awal laporan, format tanggal/angka, dan label Bahasa Indonesia tersedia. Tes unit periode serta UI setelah Activity dibuat ulang lulus.
- [x] 11.5 Lengkapi S-22 detail saldo/riwayat dompet dan S-23 daftar dompet terarsip; verifikasi riwayat terarsip tetap dapat dibuka dan tidak tersedia untuk transaksi baru. Detail menghitung saldo dari ledger dan menampilkan riwayat; daftar arsip hanya baca. Tes UI AVD lulus.
- [x] 11.6 Implementasikan rincian kategori S-53 dari laporan periode; verifikasi hanya transaksi kategori dan rentang periode terpilih yang tampil. Query memakai split `countsInReport`; dialog menampilkan baris transaksi kategori dalam periode aktif. Tes DAO dan UI AVD lulus.
- [x] 11.7 Implementasikan detail kontak S-71 dengan total dua arah dan riwayat catatan; verifikasi data hanya berasal dari kontak terpilih. Dialog merinci piutang, utang, sisa pokok, dan catatan; tes UI AVD lulus.
- [x] 11.8 Implementasikan mode cek mutasi S-102; verifikasi status cocok tersimpan tanpa mengubah nominal atau saldo ledger. Hasil cocok/tidak cocok terikat ke checkpoint melalui tabel `reconciliation_items`, migrasi 20→21; tes UI mengonfirmasi nominal, saldo, dan transaksi ledger tetap sama.
- [x] 11.9 Audit keterlacakan semua requirement dan layar terhadap tes yang tersedia; verifikasi matriks requirement → use case/layar → tes tidak memiliki item tanpa pemilik. Matriks mencakup 44 requirement dan 62 layar; semua memiliki use case/implementasi serta tes atau bukti review. Catatan: `task-11.9-traceability.md`.
- [x] 11.10 Jalankan build bersih, seluruh tes, pemeriksaan migrasi, dan uji instalasi API minimum; verifikasi hasil dapat direproduksi dari dokumentasi proyek. `./gradlew clean :app:testDebugUnitTest :app:connectedDebugAndroidTest -Pandroid.injected.device.serial=emulator-5554` lulus; 112 tes instrumentasi lulus, termasuk migrasi 19→21. `:app:installDebug` berhasil pada AVD API 26 dan MainActivity terbuka tanpa crash.
- [x] 11.11 Siapkan catatan rilis dan prosedur upgrade/rollback yang tidak menghapus database pengguna; verifikasi langkah rilis diuji pada database fixture versi sebelumnya. `release-notes.md` melarang downgrade APK lama di atas schema baru dan mengarahkan pemulihan ke hotfix forward-only. Fixture schema 20→21 lulus lewat `DatabaseMigrationTest`.

## Workflow follow-up

- Arsipkan perubahan setelah implementasi seluruh scope yang dipilih dan hasil verifikasi ditinjau.
- Fitur fase 2–4 dapat dijadwalkan sebagai perubahan OpenSpec terpisah jika tidak dikerjakan dalam satu siklus rilis.
